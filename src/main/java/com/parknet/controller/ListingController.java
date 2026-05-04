package com.parknet.controller;

import com.parknet.dto.ListingForm;
import com.parknet.dto.ReservationRequest;
import com.parknet.model.ParkingListing;
import com.parknet.model.UserAccount;
import com.parknet.service.InvalidImageException;
import com.parknet.service.ListingMapService;
import com.parknet.service.ParkingListingService;
import com.parknet.service.UserAccountService;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Objects;

@Controller
public class ListingController {

    private final ParkingListingService parkingListingService;
    private final ListingMapService listingMapService;
    private final UserAccountService userAccountService;

    public ListingController(
            ParkingListingService parkingListingService,
            ListingMapService listingMapService,
            UserAccountService userAccountService
    ) {
        this.parkingListingService = parkingListingService;
        this.listingMapService = listingMapService;
        this.userAccountService = userAccountService;
    }

    @GetMapping("/listings/{id}")
    public String details(
            @PathVariable Long id,
            @ModelAttribute("reservationRequest") ReservationRequest reservationRequest,
            Authentication authentication,
            Model model
    ) {
        ParkingListing listing = parkingListingService.getActiveListing(id);
        prepareDetailsModel(model, listing, authentication);
        boolean canReserve = isAuthenticated(authentication) && !ownsListing(listing, authentication);
        model.addAttribute("listingMapPoint", listingMapService.toMapListing(listing, null, currentUserEmail(authentication)));
        model.addAttribute("listingDetailsMap", listingMapService.toDetailsMap(
                listing,
                null,
                currentUserEmail(authentication),
                canReserve
        ));
        return "listings/details";
    }

    @GetMapping("/listings/new")
    public String createForm(@ModelAttribute("listingForm") ListingForm listingForm, Model model) {
        prepareCreateFormModel(model);
        return "listings/form";
    }

    @PostMapping("/listings")
    public String create(
            @Valid @ModelAttribute("listingForm") ListingForm listingForm,
            BindingResult bindingResult,
            Model model
    ) {
        if (bindingResult.hasErrors()) {
            prepareCreateFormModel(model);
            return "listings/form";
        }

        UserAccount owner = userAccountService.currentUser();
        try {
            ParkingListing listing = parkingListingService.createListing(listingForm, owner);
            return "redirect:/listings/" + listing.getId();
        } catch (InvalidImageException ex) {
            addImageError(bindingResult, listingForm, ex);
            prepareCreateFormModel(model);
            return "listings/form";
        } catch (IllegalArgumentException ex) {
            addListingValidationError(bindingResult, listingForm, ex);
            prepareCreateFormModel(model);
            return "listings/form";
        }
    }

    @GetMapping("/listings/{id}/edit")
    public String editForm(
            @PathVariable Long id,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        UserAccount currentUser = userAccountService.currentUser();
        try {
            ParkingListing listing = parkingListingService.getListingForManagement(id, currentUser);
            if (!model.containsAttribute("listingForm")) {
                model.addAttribute("listingForm", ListingForm.fromListing(listing));
            }
            prepareEditFormModel(model, listing);
            return "listings/form";
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("listingError", ex.getMessage());
            return "redirect:/my-listings";
        }
    }

    @PostMapping("/listings/{id}/edit")
    public String update(
            @PathVariable Long id,
            @Valid @ModelAttribute("listingForm") ListingForm listingForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        UserAccount currentUser = userAccountService.currentUser();
        ParkingListing listing;
        try {
            listing = parkingListingService.getListingForManagement(id, currentUser);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("listingError", ex.getMessage());
            return "redirect:/my-listings";
        }

        if (bindingResult.hasErrors()) {
            prepareEditFormModel(model, listing);
            return "listings/form";
        }

        try {
            parkingListingService.updateListing(id, listingForm, currentUser);
            redirectAttributes.addFlashAttribute("listingSuccess", "Обявата е обновена успешно.");
            return "redirect:/my-listings";
        } catch (InvalidImageException ex) {
            addImageError(bindingResult, listingForm, ex);
            prepareEditFormModel(model, listing);
            return "listings/form";
        } catch (IllegalArgumentException ex) {
            addListingValidationError(bindingResult, listingForm, ex);
            prepareEditFormModel(model, listing);
            return "listings/form";
        } catch (IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("listingError", ex.getMessage());
            return "redirect:/my-listings";
        }
    }

    @PostMapping("/listings/{id}/delete")
    public String delete(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes
    ) {
        UserAccount currentUser = userAccountService.currentUser();
        try {
            parkingListingService.softDeleteListing(id, currentUser);
            redirectAttributes.addFlashAttribute("listingSuccess", "Обявата е деактивирана успешно.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("listingError", ex.getMessage());
        }
        return "redirect:/my-listings";
    }

    public static void prepareDetailsModel(Model model, ParkingListing listing, Authentication authentication) {
        model.addAttribute("listing", listing);
        model.addAttribute("ownsListing", ownsListing(listing, authentication));
    }

    public static boolean ownsListing(ParkingListing listing, Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return false;
        }
        return Objects.equals(listing.getOwner().getEmail(), authentication.getName());
    }

    private static boolean isAuthenticated(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }

    private static String currentUserEmail(Authentication authentication) {
        if (!isAuthenticated(authentication)) {
            return null;
        }
        return authentication.getName();
    }

    private void prepareCreateFormModel(Model model) {
        model.addAttribute("editMode", false);
        model.addAttribute("formAction", "/listings");
        model.addAttribute("formTitle", "Публикувай гараж или паркомясто");
        model.addAttribute("formIntro", "Попълни данните ясно, за да могат наемателите бързо да преценят дали мястото им върши работа.");
        model.addAttribute("submitLabel", "Публикувай");
    }

    private void prepareEditFormModel(Model model, ParkingListing listing) {
        model.addAttribute("editMode", true);
        model.addAttribute("listing", listing);
        model.addAttribute("formAction", "/listings/" + listing.getId() + "/edit");
        model.addAttribute("formTitle", "Редактирай обява");
        model.addAttribute("formIntro", "Обнови данните за мястото, цената и периода на наличност.");
        model.addAttribute("submitLabel", "Запази промените");
    }

    private void addListingValidationError(
            BindingResult bindingResult,
            ListingForm listingForm,
            IllegalArgumentException ex
    ) {
        String message = ex.getMessage();
        if (message != null && (message.contains("Цената") || message.contains("цена на ден"))) {
            addFieldError(bindingResult, "pricePerDay", listingForm.getPricePerDay(), message);
        } else if (message != null && (message.contains("Почасовата") || message.contains("почасова цена"))) {
            addFieldError(bindingResult, "pricePerHour", listingForm.getPricePerHour(), message);
        } else if (message != null && message.contains("ценообразуване")) {
            addFieldError(bindingResult, "pricingType", listingForm.getPricingType(), message);
        } else if (message != null && (message.contains("Координат") || message.contains("точна позиция"))) {
            addFieldError(bindingResult, "latitude", listingForm.getLatitude(), message);
        } else if (message != null
                && (message.contains("GeoJSON")
                || message.contains("границ")
                || message.contains("точки")
                || message.contains("София"))) {
            addFieldError(bindingResult, "geometryGeoJson", listingForm.getGeometryGeoJson(), message);
        } else {
            addFieldError(bindingResult, "availableTo", listingForm.getAvailableTo(), message);
        }
    }

    private void addImageError(
            BindingResult bindingResult,
            ListingForm listingForm,
            InvalidImageException ex
    ) {
        bindingResult.addError(new FieldError(
                "listingForm",
                "imageFile",
                listingForm.getImageFile(),
                false,
                null,
                null,
                ex.getMessage()
        ));
    }

    private void addFieldError(BindingResult bindingResult, String fieldName, Object rejectedValue, String message) {
        bindingResult.addError(new FieldError(
                "listingForm",
                fieldName,
                rejectedValue,
                false,
                null,
                null,
                message
        ));
    }
}
