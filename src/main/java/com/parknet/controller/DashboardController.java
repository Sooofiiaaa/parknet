package com.parknet.controller;

import com.parknet.model.UserAccount;
import com.parknet.model.ParkingListing;
import com.parknet.service.ListingMapService;
import com.parknet.service.ParkingListingService;
import com.parknet.service.ReservationService;
import com.parknet.service.UserAccountService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class DashboardController {

    private final ParkingListingService parkingListingService;
    private final ListingMapService listingMapService;
    private final ReservationService reservationService;
    private final UserAccountService userAccountService;

    public DashboardController(
            ParkingListingService parkingListingService,
            ListingMapService listingMapService,
            ReservationService reservationService,
            UserAccountService userAccountService
    ) {
        this.parkingListingService = parkingListingService;
        this.listingMapService = listingMapService;
        this.reservationService = reservationService;
        this.userAccountService = userAccountService;
    }

    @GetMapping("/my-listings")
    public String myListings(Model model) {
        UserAccount currentUser = userAccountService.currentUser();
        List<ParkingListing> listings = parkingListingService.findByOwner(currentUser);
        model.addAttribute("listings", listings);
        model.addAttribute("listingMapPoints", listingMapService.toManagementMapListings(
                listings,
                currentUser.getEmail()
        ));
        return "dashboard/my-listings";
    }

    @GetMapping("/my-reservations")
    public String myReservations(Model model) {
        UserAccount currentUser = userAccountService.currentUser();
        model.addAttribute("reservations", reservationService.findByRenter(currentUser));
        return "dashboard/my-reservations";
    }
}
