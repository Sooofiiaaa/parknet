package com.parknet.dto;

public class ListingDetailsMapDto {

    private MapListingDto listing;
    private String fullDescription;
    private String phone;
    private String ownerName;
    private boolean canReserve;

    public ListingDetailsMapDto() {
    }

    public ListingDetailsMapDto(
            MapListingDto listing,
            String fullDescription,
            String phone,
            String ownerName,
            boolean canReserve
    ) {
        this.listing = listing;
        this.fullDescription = fullDescription;
        this.phone = phone;
        this.ownerName = ownerName;
        this.canReserve = canReserve;
    }

    public MapListingDto getListing() {
        return listing;
    }

    public void setListing(MapListingDto listing) {
        this.listing = listing;
    }

    public String getFullDescription() {
        return fullDescription;
    }

    public void setFullDescription(String fullDescription) {
        this.fullDescription = fullDescription;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public boolean isCanReserve() {
        return canReserve;
    }

    public void setCanReserve(boolean canReserve) {
        this.canReserve = canReserve;
    }
}
