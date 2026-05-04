package com.parknet.controller;

import com.parknet.dto.ReservationRequest;
import com.parknet.model.ParkingListing;
import com.parknet.model.Reservation;
import com.parknet.model.UserAccount;
import com.parknet.service.ListingMapService;
import com.parknet.service.ParkingListingService;
import com.parknet.service.ReservationService;
import com.parknet.service.UserAccountService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ReservationController {

    private final ReservationService reservationService;
    private final ParkingListingService parkingListingService;
    private final ListingMapService listingMapService;
    private final UserAccountService userAccountService;

    public ReservationController(
            ReservationService reservationService,
            ParkingListingService parkingListingService,
            ListingMapService listingMapService,
            UserAccountService userAccountService
    ) {
        this.reservationService = reservationService;
        this.parkingListingService = parkingListingService;
        this.listingMapService = listingMapService;
        this.userAccountService = userAccountService;
    }

    @PostMapping("/listings/{id}/reserve")
    public String reserve(
            @PathVariable Long id,
            @Valid @ModelAttribute("reservationRequest") ReservationRequest reservationRequest,
            BindingResult bindingResult,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        ParkingListing listing = parkingListingService.getActiveListing(id);
        if (bindingResult.hasErrors()) {
            ListingController.prepareDetailsModel(model, listing, authentication);
            prepareListingMapModel(model, listing, authentication);
            return "listings/details";
        }

        try {
            UserAccount renter = userAccountService.currentUser();
            Reservation reservation = reservationService.createReservation(id, reservationRequest, renter);
            redirectAttributes.addFlashAttribute(
                    "reservationSuccess",
                    "Заявката за резервация #" + reservation.getId() + " е изпратена."
            );
            return "redirect:/listings/" + id;
        } catch (IllegalArgumentException | IllegalStateException ex) {
            model.addAttribute("reservationError", ex.getMessage());
            ListingController.prepareDetailsModel(model, listing, authentication);
            prepareListingMapModel(model, listing, authentication);
            return "listings/details";
        }
    }

    @GetMapping({"/reservation-requests", "/owner/reservations"})
    public String ownerReservationRequests(Model model) {
        UserAccount owner = userAccountService.currentUser();
        model.addAttribute("reservations", reservationService.findRequestsForOwner(owner));
        return "dashboard/reservation-requests";
    }

    @PostMapping("/reservations/{id}/confirm")
    public String confirm(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes
    ) {
        UserAccount currentUser = userAccountService.currentUser();
        try {
            reservationService.confirmReservation(id, currentUser);
            redirectAttributes.addFlashAttribute("reservationSuccess", "Заявката е потвърдена.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("reservationError", ex.getMessage());
        }
        return "redirect:/reservation-requests";
    }

    @PostMapping("/reservations/{id}/cancel")
    public String cancel(
            @PathVariable Long id,
            @RequestParam(defaultValue = "/my-reservations") String returnTo,
            RedirectAttributes redirectAttributes
    ) {
        UserAccount currentUser = userAccountService.currentUser();
        try {
            reservationService.cancelReservation(id, currentUser);
            redirectAttributes.addFlashAttribute("reservationSuccess", "Резервацията е отказана.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("reservationError", ex.getMessage());
        }
        return "redirect:" + safeReturnPath(returnTo);
    }

    private String safeReturnPath(String returnTo) {
        if ("/reservation-requests".equals(returnTo) || "/my-reservations".equals(returnTo)) {
            return returnTo;
        }
        return "/my-reservations";
    }

    private void prepareListingMapModel(Model model, ParkingListing listing, Authentication authentication) {
        String currentUserEmail = authentication == null ? null : authentication.getName();
        boolean canReserve = authentication != null
                && authentication.isAuthenticated()
                && !ListingController.ownsListing(listing, authentication);
        model.addAttribute("listingMapPoint", listingMapService.toMapListing(listing, null, currentUserEmail));
        model.addAttribute("listingDetailsMap", listingMapService.toDetailsMap(
                listing,
                null,
                currentUserEmail,
                canReserve
        ));
    }
}
