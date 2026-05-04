package com.parknet.service;

import com.parknet.dto.ListingFilter;
import com.parknet.dto.ListingForm;
import com.parknet.model.District;
import com.parknet.model.ParkingListing;
import com.parknet.model.UserAccount;
import com.parknet.repository.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class ParkingListingServiceTest {

    @Autowired
    private ParkingListingService parkingListingService;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Test
    void createListingCreatesActiveListingForOwner() {
        UserAccount owner = userAccountRepository.findByEmail("iva@example.com").orElseThrow();
        ListingForm form = validListingForm();

        ParkingListing listing = parkingListingService.createListing(form, owner);

        assertThat(listing.getId()).isNotNull();
        assertThat(listing.isActive()).isTrue();
        assertThat(listing.getOwner().getEmail()).isEqualTo(owner.getEmail());
        assertThat(listing.getTitle()).isEqualTo("Тестово място");
        assertThat(listing.getDistrict()).isEqualTo(District.CENTER);
        assertThat(listing.getGeometryGeoJson()).contains("\"Polygon\"");
        assertThat(listing.getCenterLatitude()).isNotNull();
        assertThat(listing.getCenterLongitude()).isNotNull();
    }

    @Test
    void createListingAssignsDistrictFromLocation() {
        UserAccount owner = userAccountRepository.findByEmail("iva@example.com").orElseThrow();
        ListingForm form = validListingForm();
        form.setDistrict(District.CENTER);
        form.setGeometryGeoJson("""
                {"type":"Polygon","coordinates":[[[23.377900,42.646400],[23.378300,42.646400],[23.378300,42.646800],[23.377900,42.646800],[23.377900,42.646400]]]}
                """);

        ParkingListing listing = parkingListingService.createListing(form, owner);

        assertThat(listing.getDistrict()).isEqualTo(District.MLADOST);
    }

    @Test
    void createListingDoesNotRequireManualDistrictSelection() {
        UserAccount owner = userAccountRepository.findByEmail("iva@example.com").orElseThrow();
        ListingForm form = validListingForm();
        form.setDistrict(null);

        ParkingListing listing = parkingListingService.createListing(form, owner);

        assertThat(listing.getDistrict()).isEqualTo(District.CENTER);
    }

    @Test
    void createListingRejectsInvalidAvailabilityRange() {
        UserAccount owner = userAccountRepository.findByEmail("iva@example.com").orElseThrow();
        ListingForm form = validListingForm();
        form.setAvailableFrom(LocalDate.now().plusDays(5));
        form.setAvailableTo(LocalDate.now().plusDays(2));

        assertThatThrownBy(() -> parkingListingService.createListing(form, owner))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Началната дата");
    }

    @Test
    void createListingRejectsNonPositivePrice() {
        UserAccount owner = userAccountRepository.findByEmail("iva@example.com").orElseThrow();
        ListingForm form = validListingForm();
        form.setPricePerHour(BigDecimal.ZERO);

        assertThatThrownBy(() -> parkingListingService.createListing(form, owner))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Почасовата");
    }

    @Test
    void createListingRejectsIncompleteCoordinates() {
        UserAccount owner = userAccountRepository.findByEmail("iva@example.com").orElseThrow();
        ListingForm form = validListingForm();
        form.setLatitude(new BigDecimal("42.700000"));

        assertThatThrownBy(() -> parkingListingService.createListing(form, owner))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("точна позиция");
    }

    @Test
    void updateListingGeometryRecalculatesCenterForOwner() {
        UserAccount owner = userAccountRepository.findByEmail("iva@example.com").orElseThrow();
        ParkingListing listing = parkingListingService.findByOwner(owner).get(0);

        ParkingListing updated = parkingListingService.updateListingGeometry(listing.getId(), """
                {"type":"Polygon","coordinates":[[[23.323000,42.698000],[23.323300,42.698000],[23.323300,42.698300],[23.323000,42.698300],[23.323000,42.698000]]]}
                """, owner);

        assertThat(updated.getGeometryGeoJson()).contains("\"Polygon\"");
        assertThat(updated.getCenterLatitude()).isCloseTo(42.69815, org.assertj.core.data.Offset.offset(0.000001));
        assertThat(updated.getCenterLongitude()).isCloseTo(23.32315, org.assertj.core.data.Offset.offset(0.000001));
    }

    @Test
    void updateListingGeometryRejectsOutsideSofiaBounds() {
        UserAccount owner = userAccountRepository.findByEmail("iva@example.com").orElseThrow();
        ParkingListing listing = parkingListingService.findByOwner(owner).get(0);

        assertThatThrownBy(() -> parkingListingService.updateListingGeometry(listing.getId(), """
                {"type":"Polygon","coordinates":[[[23.300000,42.600000],[23.900000,42.600000],[23.400000,42.700000],[23.300000,42.600000]]]}
                """, owner))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("София");
    }

    @Test
    void districtMapCentersContainsEveryDistrict() {
        assertThat(parkingListingService.districtMapCenters())
                .hasSize(District.values().length)
                .allSatisfy(center -> {
                    assertThat(center.getCode()).isNotBlank();
                    assertThat(center.getDisplayName()).isNotBlank();
                    assertThat(center.getLatitude()).isNotNull();
                    assertThat(center.getLongitude()).isNotNull();
                });
    }

    @Test
    void filterListingsByDistrict() {
        ListingFilter filter = new ListingFilter();
        filter.setDistrict(District.LOZENETS);

        List<ParkingListing> results = parkingListingService.findActiveListings(filter);

        assertThat(results).isNotEmpty();
        assertThat(results)
                .allSatisfy(listing -> {
                    assertThat(listing.isActive()).isTrue();
                    assertThat(listing.getDistrict()).isEqualTo(District.LOZENETS);
                });
    }

    @Test
    void filterListingsByMaxPrice() {
        ListingFilter filter = new ListingFilter();
        filter.setMaxPrice(new BigDecimal("14.00"));

        List<ParkingListing> results = parkingListingService.findActiveListings(filter);

        assertThat(results).isNotEmpty();
        assertThat(results)
                .allSatisfy(listing -> {
                    assertThat(listing.isActive()).isTrue();
                    boolean hourlyMatches = listing.getPricePerHour() != null
                            && listing.getPricePerHour().compareTo(filter.getMaxPrice()) <= 0;
                    assertThat(hourlyMatches).isTrue();
                });
    }

    @Test
    void filterListingsByDateRange() {
        ListingFilter filter = new ListingFilter();
        filter.setDateFrom(LocalDate.now().plusDays(2));
        filter.setDateTo(LocalDate.now().plusDays(4));

        List<ParkingListing> results = parkingListingService.findActiveListings(filter);

        assertThat(results).isNotEmpty();
        assertThat(results)
                .allSatisfy(listing -> {
                    assertThat(listing.isActive()).isTrue();
                    assertThat(listing.getAvailableFrom()).isBeforeOrEqualTo(filter.getDateFrom());
                    assertThat(listing.getAvailableTo()).isAfterOrEqualTo(filter.getDateTo());
                });
    }

    private ListingForm validListingForm() {
        ListingForm form = new ListingForm();
        form.setTitle("Тестово място");
        form.setDescription("Тестово описание на мястото.");
        form.setDistrict(District.CENTER);
        form.setAddress("ул. Тестова 1, София");
        form.setPricePerHour(new BigDecimal("2.00"));
        form.setPricePerDay(new BigDecimal("15.00"));
        form.setCurrency("€");
        form.setAvailableFrom(LocalDate.now());
        form.setAvailableTo(LocalDate.now().plusDays(3));
        form.setPhone("+359 88 111 1111");
        form.setImagePath(null);
        form.setGeometryGeoJson("""
                {"type":"Polygon","coordinates":[[[23.321700,42.697600],[23.322000,42.697600],[23.322000,42.697900],[23.321700,42.697900],[23.321700,42.697600]]]}
                """);
        return form;
    }
}
