package com.parknet.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GeoJsonServiceTest {

    private final GeoJsonService geoJsonService = new GeoJsonService(new ObjectMapper());

    @Test
    void validatesPolygonAndCalculatesCenter() {
        GeoJsonService.ValidatedGeometry result = geoJsonService.validateAndCalculateCenter("""
                {"type":"Polygon","coordinates":[[[23.300000,42.600000],[23.400000,42.600000],[23.400000,42.700000],[23.300000,42.700000],[23.300000,42.600000]]]}
                """);

        assertThat(result.geometryGeoJson()).contains("\"Polygon\"");
        assertThat(result.centerLatitude()).isCloseTo(42.65, org.assertj.core.data.Offset.offset(0.000001));
        assertThat(result.centerLongitude()).isCloseTo(23.35, org.assertj.core.data.Offset.offset(0.000001));
    }

    @Test
    void rejectsMissingGeoJson() {
        assertThatThrownBy(() -> geoJsonService.validateAndCalculateCenter(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("граници");
    }

    @Test
    void rejectsInvalidJsonSafely() {
        assertThatThrownBy(() -> geoJsonService.validateAndCalculateCenter("{bad json"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("валиден JSON");
    }

    @Test
    void rejectsTooFewPoints() {
        assertThatThrownBy(() -> geoJsonService.validateAndCalculateCenter("""
                {"type":"Polygon","coordinates":[[[23.300000,42.600000],[23.400000,42.600000],[23.300000,42.600000]]]}
                """))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("поне 3 точки");
    }

    @Test
    void rejectsCoordinatesOutsideSofiaBounds() {
        assertThatThrownBy(() -> geoJsonService.validateAndCalculateCenter("""
                {"type":"Polygon","coordinates":[[[23.300000,42.600000],[23.900000,42.600000],[23.400000,42.700000],[23.300000,42.600000]]]}
                """))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("София");
    }
}
