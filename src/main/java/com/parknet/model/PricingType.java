package com.parknet.model;

public enum PricingType {
    HOURLY("Почасово"),
    DAILY("На ден"),
    HOURLY_AND_DAILY("Почасово и на ден");

    private final String displayName;

    PricingType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
