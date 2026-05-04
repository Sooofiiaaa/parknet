package com.parknet.repository;

import com.parknet.model.Reservation;
import com.parknet.model.ReservationStatus;
import com.parknet.model.UserAccount;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    @EntityGraph(attributePaths = {"listing", "listing.owner"})
    List<Reservation> findByRenterOrderByCreatedAtDesc(UserAccount renter);

    @EntityGraph(attributePaths = {"listing", "renter"})
    List<Reservation> findByListingOwnerOrderByCreatedAtDesc(UserAccount owner);

    @Query("""
            select count(r)
            from Reservation r
            where r.listing.id = :listingId
              and r.status = :status
              and (:excludedReservationId is null or r.id <> :excludedReservationId)
              and r.startDate <= :endDate
              and r.endDate >= :startDate
            """)
    long countOverlappingReservations(
            @Param("listingId") Long listingId,
            @Param("status") ReservationStatus status,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("excludedReservationId") Long excludedReservationId
    );

    @EntityGraph(attributePaths = {"listing", "renter"})
    @Query("""
            select r
            from Reservation r
            where r.listing.id = :listingId
              and r.status = com.parknet.model.ReservationStatus.REQUESTED
              and r.id <> :excludedReservationId
              and r.startDate <= :endDate
              and r.endDate >= :startDate
            """)
    List<Reservation> findOverlappingRequestedReservations(
            @Param("listingId") Long listingId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("excludedReservationId") Long excludedReservationId
    );
}
