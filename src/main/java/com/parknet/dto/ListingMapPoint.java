package com.parknet.dto;

import java.math.BigDecimal;

public class ListingMapPoint {

    private Long id;
    private String title;
    private String districtName;
    private String address;
    private String pricePerDay;
    private String availableFrom;
    private String availableTo;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String status;
    private String statusLabel;
    private String color;
    private String detailsUrl;
    private String editUrl;

    public ListingMapPoint() {
    }

    public ListingMapPoint(
            Long id,
            String title,
            String districtName,
            String address,
            String pricePerDay,
            String availableFrom,
            String availableTo,
            BigDecimal latitude,
            BigDecimal longitude,
            String status,
            String statusLabel,
            String color,
            String detailsUrl
    ) {
        this(
                id,
                title,
                districtName,
                address,
                pricePerDay,
                availableFrom,
                availableTo,
                latitude,
                longitude,
                status,
                statusLabel,
                color,
                detailsUrl,
                null
        );
    }

    public ListingMapPoint(
            Long id,
            String title,
            String districtName,
            String address,
            String pricePerDay,
            String availableFrom,
            String availableTo,
            BigDecimal latitude,
            BigDecimal longitude,
            String status,
            String statusLabel,
            String color,
            String detailsUrl,
            String editUrl
    ) {
        this.id = id;
        this.title = title;
        this.districtName = districtName;
        this.address = address;
        this.pricePerDay = pricePerDay;
        this.availableFrom = availableFrom;
        this.availableTo = availableTo;
        this.latitude = latitude;
        this.longitude = longitude;
        this.status = status;
        this.statusLabel = statusLabel;
        this.color = color;
        this.detailsUrl = detailsUrl;
        this.editUrl = editUrl;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDistrictName() {
        return districtName;
    }

    public void setDistrictName(String districtName) {
        this.districtName = districtName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPricePerDay() {
        return pricePerDay;
    }

    public void setPricePerDay(String pricePerDay) {
        this.pricePerDay = pricePerDay;
    }

    public String getAvailableFrom() {
        return availableFrom;
    }

    public void setAvailableFrom(String availableFrom) {
        this.availableFrom = availableFrom;
    }

    public String getAvailableTo() {
        return availableTo;
    }

    public void setAvailableTo(String availableTo) {
        this.availableTo = availableTo;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatusLabel() {
        return statusLabel;
    }

    public void setStatusLabel(String statusLabel) {
        this.statusLabel = statusLabel;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getDetailsUrl() {
        return detailsUrl;
    }

    public void setDetailsUrl(String detailsUrl) {
        this.detailsUrl = detailsUrl;
    }

    public String getEditUrl() {
        return editUrl;
    }

    public void setEditUrl(String editUrl) {
        this.editUrl = editUrl;
    }
}
