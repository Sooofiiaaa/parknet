package com.parknet.service;

import com.parknet.dto.ReservationRequest;
import com.parknet.model.District;
import com.parknet.model.ParkingListing;
import com.parknet.model.Reservation;
import com.parknet.model.ReservationStatus;
import com.parknet.model.UserAccount;
import com.parknet.repository.ParkingListingRepository;
import com.parknet.repository.ReservationRepository;
import com.parknet.repository.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class ReservationServiceTest {

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private ParkingListingRepository parkingListingRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Test
    void createReservationRejectsOwnListing() {
        UserAccount owner = userAccountRepository.findByEmail("iva@example.com").orElseThrow();
        ParkingListing listing = centerListingForOwner(owner);
        ReservationRequest request = reservationRequest(LocalDate.now().plusDays(1), LocalDate.now().plusDays(2));

        assertThatThrownBy(() -> reservationService.createReservation(listing.getId(), request, owner))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("собствена обява");
    }

    @Test
    void createReservationCalculatesInclusiveDailyPrice() {
        UserAccount renter = userAccountRepository.findByEmail("georgi@example.com").orElseThrow();
        ParkingListing listing = parkingListingRepository.findActiveWithFilters(
                District.CENTER,
                LocalDate.now().plusDays(1),
                new BigDecimal("40.00")
        ).getFirst();
        listing.setDemoListing(true);
        parkingListingRepository.save(listing);
        ReservationRequest request = reservationRequest(LocalDate.now().plusDays(1), LocalDate.now().plusDays(3));

        Reservation reservation = reservationService.createReservation(listing.getId(), request, renter);
        BigDecimal expectedDailyPrice = listing.getPricePerDay() == null
                ? listing.getPricePerHour().multiply(new BigDecimal("24"))
                : listing.getPricePerDay();

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(reservation.getTotalPrice())
                .isEqualByComparingTo(expectedDailyPrice.multiply(new BigDecimal("3")));
    }

    @Test
    void createReservationForNormalListingStillWaitsForApproval() {
        UserAccount renter = userAccountRepository.findByEmail("georgi@example.com").orElseThrow();
        ParkingListing listing = centerListing();
        listing.setDemoListing(false);
        listing.setImagePath(null);
        parkingListingRepository.save(listing);
        ReservationRequest request = reservationRequest(LocalDate.now().plusDays(20), LocalDate.now().plusDays(20));

        Reservation reservation = reservationService.createReservation(listing.getId(), request, renter);

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.REQUESTED);
    }

    @Test
    void createReservationRejectsDatesOutsideListingAvailability() {
        UserAccount renter = userAccountRepository.findByEmail("georgi@example.com").orElseThrow();
        ParkingListing listing = parkingListingRepository.findActiveWithFilters(
                District.CENTER,
                LocalDate.now().plusDays(1),
                new BigDecimal("40.00")
        ).getFirst();
        ReservationRequest request = reservationRequest(listing.getAvailableFrom().minusDays(1), listing.getAvailableFrom());

        assertThatThrownBy(() -> reservationService.createReservation(listing.getId(), request, renter))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("рамките на наличността");
    }

    @Test
    void createReservationRejectsOverlapWithConfirmedReservation() {
        UserAccount owner = userAccountRepository.findByEmail("iva@example.com").orElseThrow();
        UserAccount renter = userAccountRepository.findByEmail("georgi@example.com").orElseThrow();
        ParkingListing listing = centerListing();
        listing.setDemoListing(false);
        listing.setImagePath(null);
        parkingListingRepository.save(listing);

        Reservation firstReservation = reservationService.createReservation(
                listing.getId(),
                reservationRequest(LocalDate.now().plusDays(8), LocalDate.now().plusDays(9)),
                renter
        );
        reservationService.confirmReservation(firstReservation.getId(), owner);

        ReservationRequest overlappingRequest = reservationRequest(
                LocalDate.now().plusDays(9),
                LocalDate.now().plusDays(10)
        );

        assertThatThrownBy(() -> reservationService.createReservation(listing.getId(), overlappingRequest, renter))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("потвърдена резервация");
    }

    @Test
    void confirmReservationCancelsOverlappingRequestedReservations() {
        UserAccount owner = userAccountRepository.findByEmail("iva@example.com").orElseThrow();
        UserAccount renter = userAccountRepository.findByEmail("georgi@example.com").orElseThrow();
        ParkingListing listing = centerListing();
        listing.setDemoListing(false);
        listing.setImagePath(null);
        parkingListingRepository.save(listing);

        Reservation firstReservation = reservationService.createReservation(
                listing.getId(),
                reservationRequest(LocalDate.now().plusDays(12), LocalDate.now().plusDays(14)),
                renter
        );
        Reservation overlappingReservation = reservationService.createReservation(
                listing.getId(),
                reservationRequest(LocalDate.now().plusDays(13), LocalDate.now().plusDays(15)),
                renter
        );

        Reservation confirmedReservation = reservationService.confirmReservation(firstReservation.getId(), owner);
        Reservation cancelledReservation = reservationRepository.findById(overlappingReservation.getId()).orElseThrow();

        assertThat(confirmedReservation.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(cancelledReservation.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
    }

    @Test
    void confirmReservationRequiresListingOwnerOrAdmin() {
        UserAccount renter = userAccountRepository.findByEmail("georgi@example.com").orElseThrow();
        ParkingListing listing = centerListing();
        listing.setDemoListing(false);
        listing.setImagePath(null);
        parkingListingRepository.save(listing);
        Reservation reservation = reservationService.createReservation(
                listing.getId(),
                reservationRequest(LocalDate.now().plusDays(20), LocalDate.now().plusDays(20)),
                renter
        );

        assertThatThrownBy(() -> reservationService.confirmReservation(reservation.getId(), renter))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Нямате права");
    }

    @Test
    void cancelReservationAllowsRenter() {
        UserAccount renter = userAccountRepository.findByEmail("georgi@example.com").orElseThrow();
        ParkingListing listing = centerListing();
        listing.setDemoListing(false);
        listing.setImagePath(null);
        parkingListingRepository.save(listing);
        Reservation reservation = reservationService.createReservation(
                listing.getId(),
                reservationRequest(LocalDate.now().plusDays(22), LocalDate.now().plusDays(23)),
                renter
        );

        Reservation cancelledReservation = reservationService.cancelReservation(reservation.getId(), renter);

        assertThat(cancelledReservation.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
    }

    private ReservationRequest reservationRequest(LocalDate startDate, LocalDate endDate) {
        ReservationRequest request = new ReservationRequest();
        request.setStartDate(startDate);
        request.setEndDate(endDate);
        return request;
    }

    private ParkingListing centerListing() {
        UserAccount owner = userAccountRepository.findByEmail("iva@example.com").orElseThrow();
        return centerListingForOwner(owner);
    }

    private ParkingListing centerListingForOwner(UserAccount owner) {
        return parkingListingRepository.findActiveWithFilters(
                        District.CENTER,
                        LocalDate.now().plusDays(30),
                        new BigDecimal("40.00")
                )
                .stream()
                .filter(listing -> listing.getOwner().getId().equals(owner.getId()))
                .findFirst()
                .orElseThrow();
    }
}
