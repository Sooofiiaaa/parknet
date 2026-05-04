package com.parknet.service;

import com.parknet.dto.ReservationRequest;
import com.parknet.model.ParkingListing;
import com.parknet.model.Reservation;
import com.parknet.model.ReservationStatus;
import com.parknet.model.Role;
import com.parknet.model.UserAccount;
import com.parknet.repository.ParkingListingRepository;
import com.parknet.repository.ReservationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;

@Service
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final ParkingListingRepository parkingListingRepository;

    public ReservationService(
            ReservationRepository reservationRepository,
            ParkingListingRepository parkingListingRepository
    ) {
        this.reservationRepository = reservationRepository;
        this.parkingListingRepository = parkingListingRepository;
    }

    @Transactional
    public Reservation createReservation(Long listingId, ReservationRequest request, UserAccount renter) {
        ParkingListing listing = parkingListingRepository.findByIdAndActiveTrue(listingId)
                .orElseThrow(() -> new IllegalArgumentException("Обявата не е намерена."));

        validateReservation(listing, request, renter);
        ensureNoConfirmedOverlap(listing, request.getStartDate(), request.getEndDate(), null);

        long days = ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate()) + 1;
        BigDecimal totalPrice = dailyReservationPrice(listing).multiply(BigDecimal.valueOf(days));

        Reservation reservation = new Reservation(
                listing,
                renter,
                request.getStartDate(),
                request.getEndDate(),
                totalPrice,
                initialStatusFor(listing)
        );
        Reservation savedReservation = reservationRepository.save(reservation);
        if (savedReservation.getStatus() == ReservationStatus.CONFIRMED) {
            cancelOverlappingRequestedReservations(savedReservation);
        }
        return savedReservation;
    }

    @Transactional(readOnly = true)
    public List<Reservation> findByRenter(UserAccount renter) {
        return reservationRepository.findByRenterOrderByCreatedAtDesc(renter);
    }

    @Transactional(readOnly = true)
    public List<Reservation> findRequestsForOwner(UserAccount owner) {
        return reservationRepository.findByListingOwnerOrderByCreatedAtDesc(owner);
    }

    @Transactional
    public Reservation confirmReservation(Long reservationId, UserAccount currentUser) {
        Reservation reservation = findReservation(reservationId);
        ensureOwnerOrAdmin(reservation, currentUser);

        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new IllegalStateException("Отказана заявка не може да бъде потвърдена.");
        }
        if (reservation.getStatus() == ReservationStatus.CONFIRMED) {
            return reservation;
        }

        ensureNoConfirmedOverlap(
                reservation.getListing(),
                reservation.getStartDate(),
                reservation.getEndDate(),
                reservation.getId()
        );

        reservation.setStatus(ReservationStatus.CONFIRMED);
        cancelOverlappingRequestedReservations(reservation);
        return reservationRepository.save(reservation);
    }

    @Transactional
    public Reservation cancelReservation(Long reservationId, UserAccount currentUser) {
        Reservation reservation = findReservation(reservationId);
        ensureRenterOwnerOrAdmin(reservation, currentUser);

        if (reservation.getStatus() != ReservationStatus.CANCELLED) {
            reservation.setStatus(ReservationStatus.CANCELLED);
            return reservationRepository.save(reservation);
        }
        return reservation;
    }

    public void validateReservation(ParkingListing listing, ReservationRequest request, UserAccount renter) {
        if (Objects.equals(listing.getOwner().getId(), renter.getId())) {
            throw new IllegalStateException("Не можете да резервирате собствена обява.");
        }
        if (request.getStartDate() == null || request.getEndDate() == null) {
            throw new IllegalArgumentException("Въведете период за резервация.");
        }
        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw new IllegalArgumentException("Началната дата трябва да е преди или равна на крайната дата.");
        }
        if (request.getStartDate().isBefore(listing.getAvailableFrom())
                || request.getEndDate().isAfter(listing.getAvailableTo())) {
            throw new IllegalArgumentException("Периодът трябва да е в рамките на наличността на обявата.");
        }
    }

    private Reservation findReservation(Long reservationId) {
        return reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("Резервацията не е намерена."));
    }

    private BigDecimal dailyReservationPrice(ParkingListing listing) {
        if (listing.getPricePerDay() != null) {
            return listing.getPricePerDay();
        }
        if (listing.getPricePerHour() != null) {
            return listing.getPricePerHour().multiply(BigDecimal.valueOf(24));
        }
        throw new IllegalStateException("Обявата няма зададена цена за резервация.");
    }

    private ReservationStatus initialStatusFor(ParkingListing listing) {
        return isHardCodedListing(listing) ? ReservationStatus.CONFIRMED : ReservationStatus.REQUESTED;
    }

    private boolean isHardCodedListing(ParkingListing listing) {
        return listing.isDemoListing()
                || (listing.getImagePath() != null && listing.getImagePath().startsWith("/images/listings/parking-"));
    }

    private void ensureNoConfirmedOverlap(
            ParkingListing listing,
            java.time.LocalDate startDate,
            java.time.LocalDate endDate,
            Long excludedReservationId
    ) {
        long overlaps = reservationRepository.countOverlappingReservations(
                listing.getId(),
                ReservationStatus.CONFIRMED,
                startDate,
                endDate,
                excludedReservationId
        );
        if (overlaps > 0) {
            throw new IllegalStateException("Има вече потвърдена резервация за този период.");
        }
    }

    private void cancelOverlappingRequestedReservations(Reservation confirmedReservation) {
        List<Reservation> overlappingRequests = reservationRepository.findOverlappingRequestedReservations(
                confirmedReservation.getListing().getId(),
                confirmedReservation.getStartDate(),
                confirmedReservation.getEndDate(),
                confirmedReservation.getId()
        );
        for (Reservation request : overlappingRequests) {
            request.setStatus(ReservationStatus.CANCELLED);
        }
        reservationRepository.saveAll(overlappingRequests);
    }

    private void ensureOwnerOrAdmin(Reservation reservation, UserAccount currentUser) {
        boolean owner = Objects.equals(reservation.getListing().getOwner().getId(), currentUser.getId());
        boolean admin = currentUser.getRole() == Role.ADMIN;
        if (!owner && !admin) {
            throw new IllegalStateException("Нямате права да потвърдите тази заявка.");
        }
    }

    private void ensureRenterOwnerOrAdmin(Reservation reservation, UserAccount currentUser) {
        boolean renter = Objects.equals(reservation.getRenter().getId(), currentUser.getId());
        boolean owner = Objects.equals(reservation.getListing().getOwner().getId(), currentUser.getId());
        boolean admin = currentUser.getRole() == Role.ADMIN;
        if (!renter && !owner && !admin) {
            throw new IllegalStateException("Нямате права да откажете тази резервация.");
        }
    }
}
