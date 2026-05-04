package com.parknet.controller;

import com.parknet.dto.ListingFilter;
import com.parknet.dto.ListingForm;
import com.parknet.dto.MapListingDto;
import com.parknet.model.ParkingListing;
import com.parknet.model.UserAccount;
import com.parknet.service.InvalidImageException;
import com.parknet.service.ListingMapService;
import com.parknet.service.ParkingListingService;
import com.parknet.service.UserAccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
public class ListingMapApiController {

    private final ParkingListingService parkingListingService;
    private final ListingMapService listingMapService;
    private final UserAccountService userAccountService;

    public ListingMapApiController(
            ParkingListingService parkingListingService,
            ListingMapService listingMapService,
            UserAccountService userAccountService
    ) {
        this.parkingListingService = parkingListingService;
        this.listingMapService = listingMapService;
        this.userAccountService = userAccountService;
    }

    @GetMapping("/api/listings/map")
    public List<MapListingDto> listingsForMap(
            @Valid @ModelAttribute ListingFilter filter,
            BindingResult bindingResult,
            Authentication authentication
    ) {
        ListingFilter appliedFilter = bindingResult.hasErrors() || filter.hasInvalidDateRange()
                ? new ListingFilter()
                : filter;
        List<ParkingListing> listings = parkingListingService.findActiveListings(appliedFilter);
        return listingMapService.toMapListings(listings, appliedFilter, currentUserEmail(authentication));
    }

    @PostMapping(value = "/api/listings/map", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createFromMap(
            @Valid @ModelAttribute ListingForm listingForm,
            BindingResult bindingResult,
            Authentication authentication
    ) {
        if (bindingResult.hasErrors()) {
            return validationErrorResponse(bindingResult);
        }
        if (listingForm.getGeometryGeoJson() == null || listingForm.getGeometryGeoJson().isBlank()) {
            return singleErrorResponse(
                    "geometryGeoJson",
                    "Очертайте границите на паркомястото върху картата.",
                    HttpStatus.BAD_REQUEST
            );
        }

        try {
            UserAccount owner = userAccountService.currentUser();
            ParkingListing listing = parkingListingService.createListing(listingForm, owner);
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(listingMapService.toMapListing(listing, null, currentUserEmail(authentication)));
        } catch (InvalidImageException ex) {
            return singleErrorResponse("imageFile", ex.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (IllegalArgumentException ex) {
            return singleErrorResponse(fieldForMessage(ex.getMessage()), ex.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/api/listings/{id}/geometry")
    public ResponseEntity<?> updateGeometry(
            @PathVariable Long id,
            @RequestParam String geometryGeoJson,
            Authentication authentication
    ) {
        try {
            UserAccount currentUser = userAccountService.currentUser();
            ParkingListing listing = parkingListingService.updateListingGeometry(id, geometryGeoJson, currentUser);
            return ResponseEntity.ok(listingMapService.toMapListing(listing, null, currentUserEmail(authentication)));
        } catch (IllegalArgumentException ex) {
            return singleErrorResponse("geometryGeoJson", ex.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (IllegalStateException ex) {
            return singleErrorResponse("geometryGeoJson", ex.getMessage(), HttpStatus.FORBIDDEN);
        }
    }

    private String currentUserEmail(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return authentication.getName();
    }

    private ResponseEntity<Map<String, Object>> validationErrorResponse(BindingResult bindingResult) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError error : bindingResult.getFieldErrors()) {
            fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", "Проверете попълнените полета.");
        body.put("fieldErrors", fieldErrors);
        return ResponseEntity.badRequest().body(body);
    }

    private ResponseEntity<Map<String, Object>> singleErrorResponse(
            String field,
            String message,
            HttpStatus status
    ) {
        String safeMessage = message == null ? "Данните не могат да бъдат записани." : message;
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        fieldErrors.put(field, safeMessage);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", safeMessage);
        body.put("fieldErrors", fieldErrors);
        return ResponseEntity.status(status).body(body);
    }

    private String fieldForMessage(String message) {
        if (message == null) {
            return "availableTo";
        }
        if (message.contains("Почасовата") || message.contains("почасова цена")) {
            return "pricePerHour";
        }
        if (message.contains("Цената") || message.contains("цена на ден")) {
            return "pricePerDay";
        }
        if (message.contains("ценообразуване")) {
            return "pricingType";
        }
        if (message.contains("GeoJSON")
                || message.contains("границ")
                || message.contains("точки")
                || message.contains("София")) {
            return "geometryGeoJson";
        }
        return "availableTo";
    }
}
