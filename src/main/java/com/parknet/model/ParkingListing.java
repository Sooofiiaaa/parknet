package com.parknet.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Entity
@Table(name = "parking_listings")
public class ParkingListing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private UserAccount owner;

    @NotBlank
    @Column(nullable = false)
    private String title;

    @NotBlank
    @Column(nullable = false, length = 1000)
    private String description;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private District district;

    @NotBlank
    @Column(nullable = false)
    private String address;

    @Positive
    @Column(precision = 10, scale = 2)
    private BigDecimal pricePerDay;

    @Positive
    @Column(precision = 10, scale = 2)
    private BigDecimal pricePerHour;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PricingType pricingType = PricingType.HOURLY;

    @NotBlank
    @Column(nullable = false, length = 8)
    private String currency = "€";

    @NotNull
    @Column(nullable = false)
    private LocalDate availableFrom;

    @NotNull
    @Column(nullable = false)
    private LocalDate availableTo;

    @NotBlank
    @Column(nullable = false)
    private String phone;

    private String imagePath;

    @NotNull
    @Column(nullable = false, precision = 9, scale = 6)
    private BigDecimal latitude;

    @NotNull
    @Column(nullable = false, precision = 9, scale = 6)
    private BigDecimal longitude;

    @NotBlank
    @Lob
    @Column(nullable = false)
    private String geometryGeoJson;

    @NotNull
    @Column(nullable = false)
    private Double centerLatitude;

    @NotNull
    @Column(nullable = false)
    private Double centerLongitude;

    private String mapColorModeOverride;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private boolean demoListing = false;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "listing")
    private List<Reservation> reservations = new ArrayList<>();

    public ParkingListing() {
    }

    public ParkingListing(
            UserAccount owner,
            String title,
            String description,
            District district,
            String address,
            BigDecimal pricePerDay,
            LocalDate availableFrom,
            LocalDate availableTo,
            String phone,
            String imagePath,
            boolean active
    ) {
        this(
                owner,
                title,
                description,
                district,
                address,
                pricePerDay,
                availableFrom,
                availableTo,
                phone,
                imagePath,
                new BigDecimal("42.697700"),
                new BigDecimal("23.321900"),
                active
        );
    }

    public ParkingListing(
            UserAccount owner,
            String title,
            String description,
            District district,
            String address,
            BigDecimal pricePerDay,
            LocalDate availableFrom,
            LocalDate availableTo,
            String phone,
            String imagePath,
            BigDecimal latitude,
            BigDecimal longitude,
            boolean active
    ) {
        this.owner = owner;
        this.title = title;
        this.description = description;
        this.district = district;
        this.address = address;
        this.pricePerDay = pricePerDay;
        this.availableFrom = availableFrom;
        this.availableTo = availableTo;
        this.phone = phone;
        this.imagePath = imagePath;
        this.latitude = latitude;
        this.longitude = longitude;
        this.centerLatitude = latitude.doubleValue();
        this.centerLongitude = longitude.doubleValue();
        this.geometryGeoJson = defaultRectangleGeoJson(centerLatitude, centerLongitude);
        this.active = active;
    }

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (pricingType == null) {
            pricingType = PricingType.HOURLY;
        }
        if (currency == null || currency.isBlank()) {
            currency = "€";
        }
        if (centerLatitude == null && latitude != null) {
            centerLatitude = latitude.doubleValue();
        }
        if (centerLongitude == null && longitude != null) {
            centerLongitude = longitude.doubleValue();
        }
        if ((geometryGeoJson == null || geometryGeoJson.isBlank())
                && centerLatitude != null
                && centerLongitude != null) {
            geometryGeoJson = defaultRectangleGeoJson(centerLatitude, centerLongitude);
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UserAccount getOwner() {
        return owner;
    }

    public void setOwner(UserAccount owner) {
        this.owner = owner;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public District getDistrict() {
        return district;
    }

    public void setDistrict(District district) {
        this.district = district;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public BigDecimal getPricePerDay() {
        return pricePerDay;
    }

    public void setPricePerDay(BigDecimal pricePerDay) {
        this.pricePerDay = pricePerDay;
    }

    public BigDecimal getPricePerHour() {
        return pricePerHour;
    }

    public void setPricePerHour(BigDecimal pricePerHour) {
        this.pricePerHour = pricePerHour;
    }

    public PricingType getPricingType() {
        return pricingType;
    }

    public void setPricingType(PricingType pricingType) {
        this.pricingType = pricingType;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public LocalDate getAvailableFrom() {
        return availableFrom;
    }

    public void setAvailableFrom(LocalDate availableFrom) {
        this.availableFrom = availableFrom;
    }

    public LocalDate getAvailableTo() {
        return availableTo;
    }

    public void setAvailableTo(LocalDate availableTo) {
        this.availableTo = availableTo;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }

    public String getGeometryGeoJson() {
        return geometryGeoJson;
    }

    public void setGeometryGeoJson(String geometryGeoJson) {
        this.geometryGeoJson = geometryGeoJson;
    }

    public Double getCenterLatitude() {
        return centerLatitude;
    }

    public void setCenterLatitude(Double centerLatitude) {
        this.centerLatitude = centerLatitude;
    }

    public Double getCenterLongitude() {
        return centerLongitude;
    }

    public void setCenterLongitude(Double centerLongitude) {
        this.centerLongitude = centerLongitude;
    }

    public String getMapColorModeOverride() {
        return mapColorModeOverride;
    }

    public void setMapColorModeOverride(String mapColorModeOverride) {
        this.mapColorModeOverride = mapColorModeOverride;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isDemoListing() {
        return demoListing;
    }

    public void setDemoListing(boolean demoListing) {
        this.demoListing = demoListing;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<Reservation> getReservations() {
        return reservations;
    }

    public void setReservations(List<Reservation> reservations) {
        this.reservations = reservations;
    }

    public void addReservation(Reservation reservation) {
        reservations.add(reservation);
        reservation.setListing(this);
    }

    private String defaultRectangleGeoJson(double centerLatitude, double centerLongitude) {
        double latitudeDelta = 0.00012;
        double longitudeDelta = 0.00018;
        double south = centerLatitude - latitudeDelta;
        double north = centerLatitude + latitudeDelta;
        double west = centerLongitude - longitudeDelta;
        double east = centerLongitude + longitudeDelta;
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
}
