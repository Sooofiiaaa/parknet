package com.parknet.controller;

import com.parknet.dto.ListingFilter;
import com.parknet.model.ParkingListing;
import com.parknet.service.ListingMapService;
import com.parknet.service.ParkingListingService;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;

@Controller
public class HomeController {

    private final ParkingListingService parkingListingService;
    private final ListingMapService listingMapService;

    public HomeController(ParkingListingService parkingListingService, ListingMapService listingMapService) {
        this.parkingListingService = parkingListingService;
        this.listingMapService = listingMapService;
    }

    @GetMapping({"/", "/listings"})
    public String listings(
            @Valid @ModelAttribute("filter") ListingFilter filter,
            BindingResult bindingResult,
            Authentication authentication,
            Model model
    ) {
        if (filter.hasInvalidDateRange()) {
            bindingResult.addError(new FieldError(
                    "filter",
                    "dateTo",
                    filter.getDateTo(),
                    false,
                    null,
                    null,
                    "Крайната дата трябва да е след или равна на началната дата."
            ));
        }
        ListingFilter appliedFilter = bindingResult.hasErrors() ? new ListingFilter() : filter;
        List<ParkingListing> listings = parkingListingService.findActiveListings(appliedFilter);
        model.addAttribute("listings", listings);
        model.addAttribute("listingMapPoints", listingMapService.toMapListings(
                listings,
                appliedFilter,
                currentUserEmail(authentication)
        ));
        return "listings/index";
    }

    private String currentUserEmail(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return authentication.getName();
    }
}
