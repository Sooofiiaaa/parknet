package com.parknet.config;

import com.parknet.model.ParkingListing;
import com.parknet.repository.ParkingListingRepository;
import com.parknet.repository.UserAccountRepository;
import com.parknet.service.GeoJsonService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class DataSeederTest {

    @Autowired
    private ParkingListingRepository parkingListingRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private GeoJsonService geoJsonService;

    @Test
    void seedsManyVisibleMapListingsWithValidGeometry() {
        List<ParkingListing> listings = parkingListingRepository.findAll();

        assertThat(userAccountRepository.count()).isGreaterThanOrEqualTo(3);
        assertThat(listings).hasSizeGreaterThanOrEqualTo(60);
        assertThat(listings)
                .allSatisfy(listing -> {
                    assertThat(listing.isActive()).isTrue();
                    assertThat(listing.getGeometryGeoJson()).isNotBlank();
                    assertThat(listing.getCenterLatitude()).isNotNull();
                    assertThat(listing.getCenterLongitude()).isNotNull();
                    assertThat(listing.getCurrency()).isEqualTo("€");
                    assertThat(listing.getPricePerHour()).isNotNull();
                    assertThat(listing.getImagePath()).isNotBlank();
                    assertThat(listing.getPricingType()).isNotNull();
                    GeoJsonService.ValidatedGeometry geometry =
                            geoJsonService.validateAndCalculateCenter(listing.getGeometryGeoJson());
                    assertThat(geometry.centerLatitude()).isCloseTo(listing.getCenterLatitude(), org.assertj.core.data.Offset.offset(0.000001));
                    assertThat(geometry.centerLongitude()).isCloseTo(listing.getCenterLongitude(), org.assertj.core.data.Offset.offset(0.000001));
                });
    }
}
