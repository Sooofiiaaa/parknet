package com.parknet.model;

public enum ListingAvailabilityStatus {
    AVAILABLE("Свободно"),
    REQUESTED("Има заявка"),
    BOOKED("Заето"),
    UNAVAILABLE("Налично по-късно"),
    OWNED_BY_CURRENT_USER("Ваша обява"),
    INACTIVE("Неактивна");

    private final String displayName;

    ListingAvailabilityStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
