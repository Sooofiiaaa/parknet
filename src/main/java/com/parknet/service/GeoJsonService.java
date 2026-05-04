package com.parknet.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class GeoJsonService {

    public static final double SOFIA_MIN_LATITUDE = 42.55;
    public static final double SOFIA_MAX_LATITUDE = 42.85;
    public static final double SOFIA_MIN_LONGITUDE = 23.05;
    public static final double SOFIA_MAX_LONGITUDE = 23.65;
    private static final double DEFAULT_RECTANGLE_LATITUDE_DELTA = 0.00012;
    private static final double DEFAULT_RECTANGLE_LONGITUDE_DELTA = 0.00018;

    private final ObjectMapper objectMapper;

    public GeoJsonService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ValidatedGeometry validateAndCalculateCenter(String geometryGeoJson) {
        if (geometryGeoJson == null || geometryGeoJson.trim().isEmpty()) {
            throw new IllegalArgumentException("Изберете граници на мястото върху картата.");
        }

        JsonNode geometryNode = parseGeometry(geometryGeoJson);
        String type = textValue(geometryNode.get("type"));
        if (!"Polygon".equals(type)) {
            throw new IllegalArgumentException("Границите трябва да са GeoJSON Polygon.");
        }

        JsonNode coordinatesNode = geometryNode.get("coordinates");
        if (coordinatesNode == null || !coordinatesNode.isArray() || coordinatesNode.size() == 0) {
            throw new IllegalArgumentException("GeoJSON Polygon трябва да съдържа координати.");
        }

        JsonNode outerRing = coordinatesNode.get(0);
        if (outerRing == null || !outerRing.isArray()) {
            throw new IllegalArgumentException("GeoJSON Polygon трябва да съдържа външен контур.");
        }

        List<GeoPoint> points = readOuterRing(outerRing);
        List<GeoPoint> uniquePoints = removeClosingPoint(points);
        if (uniquePoints.size() < 3) {
            throw new IllegalArgumentException("Границите трябва да имат поне 3 точки.");
        }

        double latitudeSum = 0;
        double longitudeSum = 0;
        for (GeoPoint point : uniquePoints) {
            latitudeSum += point.latitude();
            longitudeSum += point.longitude();
        }

        try {
            return new ValidatedGeometry(
                    objectMapper.writeValueAsString(geometryNode),
                    latitudeSum / uniquePoints.size(),
                    longitudeSum / uniquePoints.size()
            );
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("GeoJSON данните не могат да бъдат записани.");
        }
    }

    public String rectangleAround(double centerLatitude, double centerLongitude) {
        validateCoordinate(centerLatitude, centerLongitude);
        double south = Math.max(SOFIA_MIN_LATITUDE, centerLatitude - DEFAULT_RECTANGLE_LATITUDE_DELTA);
        double north = Math.min(SOFIA_MAX_LATITUDE, centerLatitude + DEFAULT_RECTANGLE_LATITUDE_DELTA);
        double west = Math.max(SOFIA_MIN_LONGITUDE, centerLongitude - DEFAULT_RECTANGLE_LONGITUDE_DELTA);
        double east = Math.min(SOFIA_MAX_LONGITUDE, centerLongitude + DEFAULT_RECTANGLE_LONGITUDE_DELTA);
        return String.format(
                Locale.US,
                "{\"type\":\"Polygon\",\"coordinates\":[[[%.6f,%.6f],[%.6f,%.6f],[%.6f,%.6f],[%.6f,%.6f],[%.6f,%.6f]]]}",
                west, south,
                east, south,
                east, north,
                west, north,
                west, south
        );
    }

    private JsonNode parseGeometry(String geometryGeoJson) {
        try {
            JsonNode root = objectMapper.readTree(geometryGeoJson);
            if (root == null || !root.isObject()) {
                throw new IllegalArgumentException("GeoJSON данните трябва да са обект.");
            }
            String type = textValue(root.get("type"));
            if ("Feature".equals(type)) {
                JsonNode geometry = root.get("geometry");
                if (geometry == null || !geometry.isObject()) {
                    throw new IllegalArgumentException("GeoJSON Feature трябва да съдържа geometry.");
                }
                return geometry;
            }
            return root;
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("GeoJSON данните не са валиден JSON.");
        }
    }

    private List<GeoPoint> readOuterRing(JsonNode outerRing) {
        List<GeoPoint> points = new ArrayList<>();
        for (JsonNode coordinateNode : outerRing) {
            if (!coordinateNode.isArray() || coordinateNode.size() < 2) {
                throw new IllegalArgumentException("Всяка GeoJSON точка трябва да има дължина и ширина.");
            }
            double longitude = numericCoordinate(coordinateNode.get(0));
            double latitude = numericCoordinate(coordinateNode.get(1));
            validateCoordinate(latitude, longitude);
            points.add(new GeoPoint(latitude, longitude));
        }
        return points;
    }

    private List<GeoPoint> removeClosingPoint(List<GeoPoint> points) {
        if (points.size() < 2) {
            return points;
        }
        GeoPoint first = points.get(0);
        GeoPoint last = points.get(points.size() - 1);
        if (Double.compare(first.latitude(), last.latitude()) == 0
                && Double.compare(first.longitude(), last.longitude()) == 0) {
            return points.subList(0, points.size() - 1);
        }
        return points;
    }

    private double numericCoordinate(JsonNode node) {
        if (node == null || !node.isNumber()) {
            throw new IllegalArgumentException("GeoJSON координатите трябва да са числа.");
        }
        double value = node.asDouble();
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("GeoJSON координатите трябва да са валидни числа.");
        }
        return value;
    }

    private void validateCoordinate(double latitude, double longitude) {
        if (latitude < SOFIA_MIN_LATITUDE
                || latitude > SOFIA_MAX_LATITUDE
                || longitude < SOFIA_MIN_LONGITUDE
                || longitude > SOFIA_MAX_LONGITUDE) {
            throw new IllegalArgumentException("Всички точки трябва да са в границите на София.");
        }
    }

    private String textValue(JsonNode node) {
        return node == null || !node.isTextual() ? null : node.asText();
    }

    public record ValidatedGeometry(String geometryGeoJson, double centerLatitude, double centerLongitude) {
    }

    private record GeoPoint(double latitude, double longitude) {
    }
}
