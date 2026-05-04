package com.parknet.service;

import com.parknet.dto.DistrictMapCenter;
import com.parknet.dto.ListingFilter;
import com.parknet.dto.ListingForm;
import com.parknet.model.District;
import com.parknet.model.ParkingListing;
import com.parknet.model.PricingType;
import com.parknet.model.Role;
import com.parknet.model.UserAccount;
import com.parknet.repository.ParkingListingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@Service
public class ParkingListingService {

    private final ParkingListingRepository parkingListingRepository;
    private final ListingImageStorageService listingImageStorageService;
    private final GeoJsonService geoJsonService;
    private static final BigDecimal SOFIA_MIN_LATITUDE = new BigDecimal("42.55");
    private static final BigDecimal SOFIA_MAX_LATITUDE = new BigDecimal("42.85");
    private static final BigDecimal SOFIA_MIN_LONGITUDE = new BigDecimal("23.05");
    private static final BigDecimal SOFIA_MAX_LONGITUDE = new BigDecimal("23.65");

    public ParkingListingService(
            ParkingListingRepository parkingListingRepository,
            ListingImageStorageService listingImageStorageService,
            GeoJsonService geoJsonService
    ) {
        this.parkingListingRepository = parkingListingRepository;
        this.listingImageStorageService = listingImageStorageService;
        this.geoJsonService = geoJsonService;
    }

    @Transactional(readOnly = true)
    public List<ParkingListing> findActiveListings(ListingFilter filter) {
        if (filter == null) {
            return parkingListingRepository.findActiveWithFilters(null, null, null);
        }
        LocalDate requestedFrom = filter.getDateFrom();
        LocalDate requestedTo = filter.getDateTo();
        if (requestedFrom == null && requestedTo != null) {
            requestedFrom = requestedTo;
        }
        if (requestedTo == null && requestedFrom != null) {
            requestedTo = requestedFrom;
        }
        return parkingListingRepository.searchActiveListings(
                filter.getDistrict(),
                filter.getMaxPrice(),
                requestedFrom,
                requestedTo
        );
    }

    @Transactional(readOnly = true)
    public ParkingListing getActiveListing(Long id) {
        return parkingListingRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new IllegalArgumentException("Обявата не е намерена."));
    }

    @Transactional(readOnly = true)
    public List<ParkingListing> findByOwner(UserAccount owner) {
        return parkingListingRepository.findByOwnerOrderByCreatedAtDesc(owner);
    }

    public List<DistrictMapCenter> districtMapCenters() {
        return Arrays.stream(District.values())
                .map(district -> {
                    Coordinates coordinates = defaultCoordinatesFor(district);
                    return new DistrictMapCenter(district, coordinates.latitude(), coordinates.longitude());
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public ParkingListing getListingForManagement(Long id, UserAccount currentUser) {
        ParkingListing listing = findListing(id);
        ensureOwnerOrAdmin(listing, currentUser);
        return listing;
    }

    @Transactional
    public ParkingListing createListing(ListingForm form, UserAccount owner) {
        validateListingForm(form);
        GeoJsonService.ValidatedGeometry geometry = geometryForForm(form);
        String imagePath = listingImageStorageService.storeListingImage(form.getImageFile());
        Coordinates coordinates = coordinatesFromGeometry(geometry);
        District assignedDistrict = districtForLocation(coordinates);

        ParkingListing listing = new ParkingListing(
                owner,
                form.getTitle().trim(),
                form.getDescription().trim(),
                assignedDistrict,
                form.getAddress().trim(),
                form.getPricePerDay(),
                form.getAvailableFrom(),
                form.getAvailableTo(),
                form.getPhone().trim(),
                imagePath,
                coordinates.latitude(),
                coordinates.longitude(),
                true
        );
        applyMapAndPricingFields(listing, form, geometry);
        return parkingListingRepository.save(listing);
    }

    @Transactional
    public ParkingListing updateListing(Long id, ListingForm form, UserAccount currentUser) {
        ParkingListing listing = findListing(id);
        ensureOwnerOrAdmin(listing, currentUser);
        validateListingForm(form);

        listing.setTitle(form.getTitle().trim());
        listing.setDescription(form.getDescription().trim());
        listing.setAddress(form.getAddress().trim());
        listing.setPricePerDay(form.getPricePerDay());
        listing.setAvailableFrom(form.getAvailableFrom());
        listing.setAvailableTo(form.getAvailableTo());
        listing.setPhone(form.getPhone().trim());
        GeoJsonService.ValidatedGeometry geometry = geometryForForm(form);
        listing.setDistrict(districtForLocation(coordinatesFromGeometry(geometry)));
        applyMapAndPricingFields(listing, form, geometry);
        String newImagePath = listingImageStorageService.storeListingImage(form.getImageFile());
        if (newImagePath != null) {
            listing.setImagePath(newImagePath);
        }
        return parkingListingRepository.save(listing);
    }

    @Transactional
    public ParkingListing updateListingGeometry(Long id, String geometryGeoJson, UserAccount currentUser) {
        ParkingListing listing = findListing(id);
        ensureOwnerOrAdmin(listing, currentUser);
        GeoJsonService.ValidatedGeometry geometry = geoJsonService.validateAndCalculateCenter(geometryGeoJson);
        applyGeometryFields(listing, geometry);
        listing.setDistrict(districtForLocation(coordinatesFromGeometry(geometry)));
        return parkingListingRepository.save(listing);
    }

    @Transactional
    public void softDeleteListing(Long id, UserAccount currentUser) {
        ParkingListing listing = findListing(id);
        ensureOwnerOrAdmin(listing, currentUser);
        listing.setActive(false);
        parkingListingRepository.save(listing);
    }

    public void validateAvailability(ListingForm form) {
        if (form.getAvailableFrom() != null
                && form.getAvailableTo() != null
                && form.getAvailableFrom().isAfter(form.getAvailableTo())) {
            throw new IllegalArgumentException("Началната дата трябва да е преди или равна на крайната дата.");
        }
    }

    private void validateListingForm(ListingForm form) {
        validateAvailability(form);
        validateCoordinates(form);
        validatePricing(form);
    }

    private void validatePricing(ListingForm form) {
        if (form.getPricingType() == null) {
            throw new IllegalArgumentException("Изберете начин на ценообразуване.");
        }
        if (form.getPricingType() == PricingType.HOURLY && !isPositive(form.getPricePerHour())) {
            throw new IllegalArgumentException("Почасовата цена трябва да е положителна.");
        }
        if (form.getPricingType() == PricingType.DAILY && !isPositive(form.getPricePerDay())) {
            throw new IllegalArgumentException("Цената на ден трябва да е положителна.");
        }
        if (form.getPricingType() == PricingType.HOURLY_AND_DAILY
                && (!isPositive(form.getPricePerHour()) || !isPositive(form.getPricePerDay()))) {
            throw new IllegalArgumentException("Попълнете положителна почасова цена и цена на ден.");
        }
    }

    private boolean isPositive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }

    private void validateCoordinates(ListingForm form) {
        boolean hasLatitude = form.getLatitude() != null;
        boolean hasLongitude = form.getLongitude() != null;
        if (hasLatitude != hasLongitude) {
            throw new IllegalArgumentException("Изберете точна позиция с ширина и дължина или оставете и двете празни.");
        }
        if (!hasLatitude) {
            return;
        }
        if (form.getLatitude().compareTo(SOFIA_MIN_LATITUDE) < 0
                || form.getLatitude().compareTo(SOFIA_MAX_LATITUDE) > 0
                || form.getLongitude().compareTo(SOFIA_MIN_LONGITUDE) < 0
                || form.getLongitude().compareTo(SOFIA_MAX_LONGITUDE) > 0) {
            throw new IllegalArgumentException("Координатите трябва да са в района на София.");
        }
    }

    private Coordinates coordinatesFromGeometry(GeoJsonService.ValidatedGeometry geometry) {
        return new Coordinates(
                BigDecimal.valueOf(geometry.centerLatitude()).setScale(6, RoundingMode.HALF_UP),
                BigDecimal.valueOf(geometry.centerLongitude()).setScale(6, RoundingMode.HALF_UP)
        );
    }

    private District districtForLocation(Coordinates coordinates) {
        District closestDistrict = District.OTHER;
        double closestDistance = Double.MAX_VALUE;
        double latitude = coordinates.latitude().doubleValue();
        double longitude = coordinates.longitude().doubleValue();

        for (District district : District.values()) {
            if (district == District.OTHER) {
                continue;
            }
            Coordinates districtCenter = defaultCoordinatesFor(district);
            double latitudeDistance = latitude - districtCenter.latitude().doubleValue();
            double longitudeDistance = longitude - districtCenter.longitude().doubleValue();
            double distance = latitudeDistance * latitudeDistance + longitudeDistance * longitudeDistance;
            if (distance < closestDistance) {
                closestDistance = distance;
                closestDistrict = district;
            }
        }

        return closestDistrict;
    }

    private GeoJsonService.ValidatedGeometry geometryForForm(ListingForm form) {
        if (form.getGeometryGeoJson() != null && !form.getGeometryGeoJson().isBlank()) {
            return geoJsonService.validateAndCalculateCenter(form.getGeometryGeoJson());
        }
        Coordinates fallbackCenter = fallbackCenterFor(form);
        String fallbackGeometry = geoJsonService.rectangleAround(
                fallbackCenter.latitude().doubleValue(),
                fallbackCenter.longitude().doubleValue()
        );
        return geoJsonService.validateAndCalculateCenter(fallbackGeometry);
    }

    private Coordinates fallbackCenterFor(ListingForm form) {
        if (form.getLatitude() != null && form.getLongitude() != null) {
            return new Coordinates(form.getLatitude(), form.getLongitude());
        }
        return defaultCoordinatesFor(form.getDistrict());
    }

    private void applyMapAndPricingFields(
            ParkingListing listing,
            ListingForm form,
            GeoJsonService.ValidatedGeometry geometry
    ) {
        applyGeometryFields(listing, geometry);
        listing.setPricePerHour(form.getPricePerHour());
        listing.setPricePerDay(form.getPricePerDay());
        listing.setPricingType(form.getPricingType());
        listing.setCurrency(currencyOrDefault(form.getCurrency()));
        listing.setMapColorModeOverride(blankToNull(form.getMapColorModeOverride()));
    }

    private void applyGeometryFields(ParkingListing listing, GeoJsonService.ValidatedGeometry geometry) {
        Coordinates coordinates = coordinatesFromGeometry(geometry);
        listing.setLatitude(coordinates.latitude());
        listing.setLongitude(coordinates.longitude());
        listing.setCenterLatitude(geometry.centerLatitude());
        listing.setCenterLongitude(geometry.centerLongitude());
        listing.setGeometryGeoJson(geometry.geometryGeoJson());
    }

    private String currencyOrDefault(String currency) {
        return "€";
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private Coordinates defaultCoordinatesFor(District district) {
        if (district == null) {
            return new Coordinates(new BigDecimal("42.697700"), new BigDecimal("23.321900"));
        }
        return switch (district) {
            case CENTER -> new Coordinates(new BigDecimal("42.697700"), new BigDecimal("23.321900"));
            case LOZENETS -> new Coordinates(new BigDecimal("42.674900"), new BigDecimal("23.320900"));
            case MLADOST -> new Coordinates(new BigDecimal("42.646700"), new BigDecimal("23.378300"));
            case STUDENTSKI -> new Coordinates(new BigDecimal("42.650900"), new BigDecimal("23.344600"));
            case LYULIN -> new Coordinates(new BigDecimal("42.716900"), new BigDecimal("23.250400"));
            case OBORISHTE -> new Coordinates(new BigDecimal("42.699400"), new BigDecimal("23.344800"));
            case PODUYANE -> new Coordinates(new BigDecimal("42.704900"), new BigDecimal("23.358300"));
            case KRASNO_SELO -> new Coordinates(new BigDecimal("42.681600"), new BigDecimal("23.286700"));
            case GEO_MILEV -> new Coordinates(new BigDecimal("42.681000"), new BigDecimal("23.365000"));
            case MANASTIRSKI_LIVADI -> new Coordinates(new BigDecimal("42.660000"), new BigDecimal("23.295000"));
            case IVAN_VAZOV -> new Coordinates(new BigDecimal("42.678000"), new BigDecimal("23.308000"));
            case BOROVO -> new Coordinates(new BigDecimal("42.670000"), new BigDecimal("23.285000"));
            case DIANABAD -> new Coordinates(new BigDecimal("42.671000"), new BigDecimal("23.352000"));
            case DRUZHBA -> new Coordinates(new BigDecimal("42.666000"), new BigDecimal("23.397000"));
            case NADEZHDA -> new Coordinates(new BigDecimal("42.727000"), new BigDecimal("23.303000"));
            case BANISHORA -> new Coordinates(new BigDecimal("42.711000"), new BigDecimal("23.315000"));
            case OVCHA_KUPEL -> new Coordinates(new BigDecimal("42.676000"), new BigDecimal("23.255000"));
            case GOTSE_DELCHEV -> new Coordinates(new BigDecimal("42.665000"), new BigDecimal("23.292000"));
            case IZTOK -> new Coordinates(new BigDecimal("42.667000"), new BigDecimal("23.351000"));
            case SVETA_TROITSA -> new Coordinates(new BigDecimal("42.704000"), new BigDecimal("23.289000"));
            case OTHER -> new Coordinates(new BigDecimal("42.697700"), new BigDecimal("23.321900"));
        };
    }

    private ParkingListing findListing(Long id) {
        return parkingListingRepository.findByIdWithOwner(id)
                .orElseThrow(() -> new IllegalArgumentException("Обявата не е намерена."));
    }

    private void ensureOwnerOrAdmin(ParkingListing listing, UserAccount currentUser) {
        boolean owner = listing.getOwner().getId().equals(currentUser.getId());
        boolean admin = currentUser.getRole() == Role.ADMIN;
        if (!owner && !admin) {
            throw new IllegalStateException("Нямате права за управление на тази обява.");
        }
    }

    private record Coordinates(BigDecimal latitude, BigDecimal longitude) {
    }

}
