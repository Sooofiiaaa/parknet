package com.parknet.dto;

import com.parknet.model.District;

import java.math.BigDecimal;

public class DistrictMapCenter {

    private String code;
    private String displayName;
    private BigDecimal latitude;
    private BigDecimal longitude;

    public DistrictMapCenter() {
    }

    public DistrictMapCenter(District district, BigDecimal latitude, BigDecimal longitude) {
        this.code = district.name();
        this.displayName = district.getDisplayName();
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
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
}
