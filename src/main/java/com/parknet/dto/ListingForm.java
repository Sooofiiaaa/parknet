package com.parknet.dto;

import com.parknet.model.District;
import com.parknet.model.ParkingListing;
import com.parknet.model.PricingType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ListingForm {

    @NotBlank(message = "Въведете заглавие.")
    @Size(max = 120, message = "Заглавието трябва да е до 120 символа.")
    private String title;

    @NotBlank(message = "Въведете описание.")
    @Size(max = 1000, message = "Описанието трябва да е до 1000 символа.")
    private String description;

    private District district;

    @NotBlank(message = "Въведете адрес.")
    @Size(max = 220, message = "Адресът трябва да е до 220 символа.")
    private String address;

    @Positive(message = "Цената трябва да е положителна.")
    private BigDecimal pricePerDay;

    @Positive(message = "Почасовата цена трябва да е положителна.")
    private BigDecimal pricePerHour;

    @NotNull(message = "Изберете начин на ценообразуване.")
    private PricingType pricingType = PricingType.HOURLY;

    @NotBlank(message = "Въведете валута.")
    @Size(max = 8, message = "Валутата трябва да е до 8 символа.")
    private String currency = "€";

    @NotNull(message = "Въведете начална дата.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate availableFrom;

    @NotNull(message = "Въведете крайна дата.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate availableTo;

    @NotBlank(message = "Въведете телефон.")
    @Size(max = 40, message = "Телефонът трябва да е до 40 символа.")
    private String phone;

    private String imagePath;

    @DecimalMin(value = "42.55", message = "Географската ширина трябва да е в района на София.")
    @DecimalMax(value = "42.85", message = "Географската ширина трябва да е в района на София.")
    private BigDecimal latitude;

    @DecimalMin(value = "23.05", message = "Географската дължина трябва да е в района на София.")
    @DecimalMax(value = "23.65", message = "Географската дължина трябва да е в района на София.")
    private BigDecimal longitude;

    @Size(max = 10000, message = "GeoJSON данните са твърде големи.")
    private String geometryGeoJson;

    @Size(max = 40, message = "Цветовият режим трябва да е до 40 символа.")
    private String mapColorModeOverride;

    private MultipartFile imageFile;

    public ListingForm() {
    }

    @AssertTrue(message = "Изберете точна позиция с ширина и дължина или оставете и двете празни.")
    public boolean isCoordinatePairValid() {
        return (latitude == null && longitude == null) || (latitude != null && longitude != null);
    }

    @AssertTrue(message = "Попълнете положителна цена според избрания тип.")
    public boolean isPricingValid() {
        if (pricingType == null) {
            return true;
        }
        return switch (pricingType) {
            case HOURLY -> isPositive(pricePerHour);
            case DAILY -> isPositive(pricePerDay);
            case HOURLY_AND_DAILY -> isPositive(pricePerHour) && isPositive(pricePerDay);
        };
    }

    public static ListingForm fromListing(ParkingListing listing) {
        ListingForm form = new ListingForm();
        form.setTitle(listing.getTitle());
        form.setDescription(listing.getDescription());
        form.setDistrict(listing.getDistrict());
        form.setAddress(listing.getAddress());
        form.setPricePerDay(listing.getPricePerDay());
        form.setPricePerHour(listing.getPricePerHour());
        form.setPricingType(listing.getPricingType() == null ? PricingType.HOURLY : listing.getPricingType());
        form.setCurrency(listing.getCurrency() == null ? "€" : listing.getCurrency());
        form.setAvailableFrom(listing.getAvailableFrom());
        form.setAvailableTo(listing.getAvailableTo());
        form.setPhone(listing.getPhone());
        form.setImagePath(listing.getImagePath());
        form.setLatitude(listing.getLatitude());
        form.setLongitude(listing.getLongitude());
        form.setGeometryGeoJson(listing.getGeometryGeoJson());
        form.setMapColorModeOverride(listing.getMapColorModeOverride());
        return form;
    }

    private boolean isPositive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
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

    public String getMapColorModeOverride() {
        return mapColorModeOverride;
    }

    public void setMapColorModeOverride(String mapColorModeOverride) {
        this.mapColorModeOverride = mapColorModeOverride;
    }

    public MultipartFile getImageFile() {
        return imageFile;
    }

    public void setImageFile(MultipartFile imageFile) {
        this.imageFile = imageFile;
    }
}
