package com.parknet.model;

public enum ReservationStatus {
    REQUESTED("Заявена"),
    CONFIRMED("Потвърдена"),
    CANCELLED("Отказана");

    private final String displayName;

    ReservationStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
