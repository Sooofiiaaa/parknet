package com.parknet.repository;

import com.parknet.model.District;
import com.parknet.model.ParkingListing;
import com.parknet.model.UserAccount;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ParkingListingRepository extends JpaRepository<ParkingListing, Long> {

    @EntityGraph(attributePaths = "owner")
    @Query("""
            select p
            from ParkingListing p
            where p.active = true
              and (:district is null or p.district = :district)
              and (
                    :maxPrice is null
                    or (p.pricePerHour is not null and p.pricePerHour <= :maxPrice)
              )
              and (:availableFrom is null or p.availableFrom <= :availableFrom)
              and (:availableTo is null or p.availableTo >= :availableTo)
            order by p.createdAt desc
            """)
    List<ParkingListing> searchActiveListings(
            @Param("district") District district,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("availableFrom") LocalDate availableFrom,
            @Param("availableTo") LocalDate availableTo
    );

    default List<ParkingListing> findActiveWithFilters(District district, LocalDate date, BigDecimal maxPrice) {
        return searchActiveListings(district, maxPrice, date, date);
    }

    @EntityGraph(attributePaths = "owner")
    Optional<ParkingListing> findByIdAndActiveTrue(Long id);

    @EntityGraph(attributePaths = "owner")
    @Query("""
            select p
            from ParkingListing p
            where p.id = :id
            """)
    Optional<ParkingListing> findByIdWithOwner(@Param("id") Long id);

    @EntityGraph(attributePaths = "owner")
    List<ParkingListing> findByOwnerOrderByCreatedAtDesc(UserAccount owner);
}
