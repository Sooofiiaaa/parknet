package com.parknet.service;

import com.parknet.dto.ListingDetailsMapDto;
import com.parknet.dto.ListingFilter;
import com.parknet.dto.MapListingDto;
import com.parknet.model.ListingAvailabilityStatus;
import com.parknet.model.ParkingListing;
import com.parknet.model.ReservationStatus;
import com.parknet.repository.ReservationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Service
public class ListingMapService {

    private final ReservationRepository reservationRepository;
    private final GeoJsonService geoJsonService;

    public ListingMapService(ReservationRepository reservationRepository, GeoJsonService geoJsonService) {
        this.reservationRepository = reservationRepository;
        this.geoJsonService = geoJsonService;
    }

    @Transactional(readOnly = true)
    public List<MapListingDto> toMapListings(
            List<ParkingListing> listings,
            ListingFilter filter,
            String currentUserEmail
    ) {
        DateRange referenceRange = resolveReferenceRange(filter);
        return listings.stream()
                .map(listing -> toMapListingDto(listing, referenceRange, currentUserEmail, true))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MapListingDto> toManagementMapListings(List<ParkingListing> listings, String currentUserEmail) {
        DateRange referenceRange = new DateRange(LocalDate.now(), LocalDate.now());
        return listings.stream()
                .map(listing -> toMapListingDto(listing, referenceRange, currentUserEmail, false))
                .toList();
    }

    @Transactional(readOnly = true)
    public MapListingDto toMapListing(ParkingListing listing, ListingFilter filter, String currentUserEmail) {
        return toMapListingDto(listing, resolveReferenceRange(filter), currentUserEmail, true);
    }

    @Transactional(readOnly = true)
    public ListingDetailsMapDto toDetailsMap(
            ParkingListing listing,
            ListingFilter filter,
            String currentUserEmail,
            boolean canReserve
    ) {
        return new ListingDetailsMapDto(
                toMapListing(listing, filter, currentUserEmail),
                listing.getDescription(),
                listing.getPhone(),
                listing.getOwner().getFullName(),
                canReserve
        );
    }

    private MapListingDto toMapListingDto(
            ParkingListing listing,
            DateRange referenceRange,
            String currentUserEmail,
            boolean showOwnedStatus
    ) {
        boolean ownedByCurrentUser = currentUserEmail != null
                && Objects.equals(listing.getOwner().getEmail(), currentUserEmail);
        ListingAvailabilityStatus status = availabilityStatusFor(
                listing,
                referenceRange,
                ownedByCurrentUser,
                showOwnedStatus
        );
        String colorCategory = colorCategoryFor(listing, status);

        MapListingDto dto = new MapListingDto();
        dto.setId(listing.getId());
        dto.setTitle(listing.getTitle());
        dto.setShortDescription(shortDescription(listing.getDescription()));
        dto.setDistrict(listing.getDistrict());
        dto.setDistrictName(listing.getDistrict().getDisplayName());
        dto.setAddress(listing.getAddress());
        dto.setPricePerHour(listing.getPricePerHour());
        dto.setPricePerDay(listing.getPricePerDay());
        dto.setPricingType(listing.getPricingType());
        dto.setCurrency(currencyOrDefault(listing));
        dto.setDisplayRateLabel(displayRateLabel(listing));
        dto.setAvailabilityStatus(status);
        dto.setStatusLabel(status.getDisplayName());
        dto.setColorCategory(colorCategory);
        dto.setColor(colorFor(colorCategory));
        dto.setCenterLatitude(centerLatitudeFor(listing));
        dto.setCenterLongitude(centerLongitudeFor(listing));
        dto.setGeometryGeoJson(geometryFor(listing));
        dto.setAvailableFrom(listing.getAvailableFrom());
        dto.setAvailableTo(listing.getAvailableTo());
        dto.setOwnedByCurrentUser(ownedByCurrentUser);
        dto.setImagePath(listing.getImagePath());
        dto.setDetailsUrl("/listings/" + listing.getId());
        dto.setEditUrl("/listings/" + listing.getId() + "/edit");
        return dto;
    }

    private ListingAvailabilityStatus availabilityStatusFor(
            ParkingListing listing,
            DateRange referenceRange,
            boolean ownedByCurrentUser,
            boolean showOwnedStatus
    ) {
        if (!listing.isActive()) {
            return ListingAvailabilityStatus.INACTIVE;
        }
        if (showOwnedStatus && ownedByCurrentUser) {
            return ListingAvailabilityStatus.OWNED_BY_CURRENT_USER;
        }
        boolean coversReferenceRange = !listing.getAvailableFrom().isAfter(referenceRange.startDate())
                && !listing.getAvailableTo().isBefore(referenceRange.endDate());
        if (!coversReferenceRange) {
            return ListingAvailabilityStatus.UNAVAILABLE;
        }

        long confirmedOverlaps = reservationRepository.countOverlappingReservations(
                listing.getId(),
                ReservationStatus.CONFIRMED,
                referenceRange.startDate(),
                referenceRange.endDate(),
                null
        );
        if (confirmedOverlaps > 0) {
            return ListingAvailabilityStatus.BOOKED;
        }

        long requestedOverlaps = reservationRepository.countOverlappingReservations(
                listing.getId(),
                ReservationStatus.REQUESTED,
                referenceRange.startDate(),
                referenceRange.endDate(),
                null
        );
        if (requestedOverlaps > 0) {
            return ListingAvailabilityStatus.REQUESTED;
        }

        return ListingAvailabilityStatus.AVAILABLE;
    }

    private DateRange resolveReferenceRange(ListingFilter filter) {
        LocalDate startDate = filter == null ? null : filter.getDateFrom();
        LocalDate endDate = filter == null ? null : filter.getDateTo();
        if (startDate == null && endDate != null) {
            startDate = endDate;
        }
        if (endDate == null && startDate != null) {
            endDate = startDate;
        }
        if (startDate == null) {
            startDate = LocalDate.now();
            endDate = startDate;
        }
        return new DateRange(startDate, endDate);
    }

    private String displayRateLabel(ParkingListing listing) {
        if (listing.getPricePerHour() != null) {
            return formatMoney(listing.getPricePerHour()) + currencyOrDefault(listing);
        }
        return "—";
    }

    private String formatMoney(BigDecimal value) {
        if (value == null) {
            return "0";
        }
        return value.stripTrailingZeros().toPlainString();
    }

    private String currencyOrDefault(ParkingListing listing) {
        if (listing.getCurrency() == null || listing.getCurrency().isBlank()) {
            return "€";
        }
        return "€";
    }

    private String colorCategoryFor(ParkingListing listing, ListingAvailabilityStatus status) {
        if (listing.getMapColorModeOverride() != null && !listing.getMapColorModeOverride().isBlank()) {
            return listing.getMapColorModeOverride().trim();
        }
        return switch (status) {
            case AVAILABLE -> "available";
            case REQUESTED -> "requested";
            case BOOKED -> "booked";
            case UNAVAILABLE -> "unavailable";
            case OWNED_BY_CURRENT_USER -> "owned";
            case INACTIVE -> "inactive";
        };
    }

    private String colorFor(String colorCategory) {
        return switch (colorCategory) {
            case "available" -> "#1f7a5b";
            case "requested" -> "#9a6b1f";
            case "booked" -> "#b3261e";
            case "unavailable" -> "#2f5f8f";
            case "owned" -> "#5f6f2a";
            case "inactive" -> "#69717a";
            default -> "#1f7a5b";
        };
    }

    private Double centerLatitudeFor(ParkingListing listing) {
        if (listing.getCenterLatitude() != null) {
            return listing.getCenterLatitude();
        }
        return listing.getLatitude() == null ? null : listing.getLatitude().doubleValue();
    }

    private Double centerLongitudeFor(ParkingListing listing) {
        if (listing.getCenterLongitude() != null) {
            return listing.getCenterLongitude();
        }
        return listing.getLongitude() == null ? null : listing.getLongitude().doubleValue();
    }

    private String geometryFor(ParkingListing listing) {
        if (listing.getGeometryGeoJson() != null && !listing.getGeometryGeoJson().isBlank()) {
            return listing.getGeometryGeoJson();
        }
        Double centerLatitude = centerLatitudeFor(listing);
        Double centerLongitude = centerLongitudeFor(listing);
        if (centerLatitude == null || centerLongitude == null) {
            return null;
        }
        try {
            return geoJsonService.rectangleAround(centerLatitude, centerLongitude);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private String shortDescription(String description) {
        if (description == null) {
            return "";
        }
        String trimmed = description.trim();
        if (trimmed.length() <= 130) {
            return trimmed;
        }
        return trimmed.substring(0, 127) + "...";
    }

    private record DateRange(LocalDate startDate, LocalDate endDate) {
    }
}
