package com.parknet.controller;

import com.parknet.model.ParkingListing;
import com.parknet.model.UserAccount;
import com.parknet.repository.ParkingListingRepository;
import com.parknet.repository.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
class WebAccessSmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private ParkingListingRepository parkingListingRepository;

    @Test
    void anonymousCanAccessHome() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("listings/index"));
    }

    @Test
    void anonymousCanFetchMapListings() throws Exception {
        mockMvc.perform(get("/api/listings/map"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"));
    }

    @Test
    void anonymousCanLoadLocalMapAssets() throws Exception {
        mockMvc.perform(get("/vendor/leaflet/leaflet.js"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/javascript"));

        mockMvc.perform(get("/vendor/leaflet/leaflet.css"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/css"));
    }

    @Test
    void anonymousCannotAccessCreateListingPage() throws Exception {
        mockMvc.perform(get("/listings/new").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @WithMockUser(username = "iva@example.com", roles = "USER")
    void loggedInUserCanAccessCreateListingPage() throws Exception {
        mockMvc.perform(get("/listings/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("listings/form"));
    }

    @Test
    void anonymousCannotCreateListingThroughMapApi() throws Exception {
        mockMvc.perform(multipart("/api/listings/map")
                        .param("title", "Ново място")
                        .param("description", "Описание на мястото.")
                        .param("district", "CENTER")
                        .param("address", "ул. Тестова 4, София")
                        .param("pricingType", "HOURLY")
                        .param("pricePerHour", "2.00")
                        .param("currency", "€")
                        .param("availableFrom", "2026-05-01")
                        .param("availableTo", "2026-05-20")
                        .param("phone", "+359 88 222 2222")
                        .param("geometryGeoJson", validGeometry())
                        .with(csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.message").value("Влезте в профила си, за да продължите."));
    }

    @Test
    @WithMockUser(username = "iva@example.com", roles = "USER")
    void loggedInUserCanCreateListingThroughMapApi() throws Exception {
        mockMvc.perform(multipart("/api/listings/map")
                        .param("title", "Ново място от карта")
                        .param("description", "Описание на място, създадено чрез очертаване на картата.")
                        .param("district", "CENTER")
                        .param("address", "ул. Тестова 4, София")
                        .param("pricingType", "HOURLY")
                        .param("pricePerHour", "2.00")
                        .param("currency", "€")
                        .param("availableFrom", "2026-05-01")
                        .param("availableTo", "2026-05-20")
                        .param("phone", "+359 88 222 2222")
                        .param("geometryGeoJson", validGeometry())
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").value("Ново място от карта"))
                .andExpect(jsonPath("$.ownedByCurrentUser").value(true))
                .andExpect(jsonPath("$.geometryGeoJson").exists());
    }

    @Test
    @WithMockUser(username = "iva@example.com", roles = "USER")
    void mapApiRequiresDrawnGeometry() throws Exception {
        mockMvc.perform(multipart("/api/listings/map")
                        .param("title", "Ново място без граници")
                        .param("description", "Описание на място без начертана форма.")
                        .param("district", "CENTER")
                        .param("address", "ул. Тестова 5, София")
                        .param("pricingType", "HOURLY")
                        .param("pricePerHour", "2.00")
                        .param("currency", "€")
                        .param("availableFrom", "2026-05-01")
                        .param("availableTo", "2026-05-20")
                        .param("phone", "+359 88 222 2222")
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.fieldErrors.geometryGeoJson").value("Очертайте границите на паркомястото върху картата."));
    }

    @Test
    @WithMockUser(username = "iva@example.com", roles = "USER")
    void loggedInUserCanCreateListingThroughProfileFormWithFallbackGeometry() throws Exception {
        mockMvc.perform(multipart("/listings")
                        .param("title", "Профилна обява")
                        .param("description", "Описание на място, създадено от профилната форма.")
                        .param("district", "LOZENETS")
                        .param("address", "ул. Тестова 8, София")
                        .param("pricingType", "HOURLY")
                        .param("pricePerHour", "2.00")
                        .param("currency", "€")
                        .param("availableFrom", "2026-05-01")
                        .param("availableTo", "2026-05-20")
                        .param("phone", "+359 88 222 2222")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/listings/*"));
    }

    @Test
    @WithMockUser(username = "iva@example.com", roles = "USER")
    void ownerCanUpdateListingGeometryThroughMapApi() throws Exception {
        UserAccount owner = userAccountRepository.findByEmail("iva@example.com").orElseThrow();
        ParkingListing listing = parkingListingRepository.findByOwnerOrderByCreatedAtDesc(owner).get(0);

        mockMvc.perform(post("/api/listings/{id}/geometry", listing.getId())
                        .param("geometryGeoJson", shiftedGeometry())
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.ownedByCurrentUser").value(true))
                .andExpect(jsonPath("$.centerLatitude").exists())
                .andExpect(jsonPath("$.centerLongitude").exists());
    }

    private String validGeometry() {
        return """
                {"type":"Polygon","coordinates":[[[23.321700,42.697600],[23.322000,42.697600],[23.322000,42.697900],[23.321700,42.697900],[23.321700,42.697600]]]}
                """;
    }

    private String shiftedGeometry() {
        return """
                {"type":"Polygon","coordinates":[[[23.323000,42.698000],[23.323300,42.698000],[23.323300,42.698300],[23.323000,42.698300],[23.323000,42.698000]]]}
                """;
    }
}
