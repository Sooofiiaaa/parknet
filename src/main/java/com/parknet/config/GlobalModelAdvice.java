package com.parknet.config;

import com.parknet.dto.DistrictMapCenter;
import com.parknet.model.District;
import com.parknet.model.PricingType;
import com.parknet.service.ParkingListingService;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@ControllerAdvice
public class GlobalModelAdvice {

    private final ParkingListingService parkingListingService;

    public GlobalModelAdvice(ParkingListingService parkingListingService) {
        this.parkingListingService = parkingListingService;
    }

    @ModelAttribute("districts")
    public District[] districts() {
        return District.values();
    }

    @ModelAttribute("pricingTypes")
    public PricingType[] pricingTypes() {
        return PricingType.values();
    }

    @ModelAttribute("districtMapCenters")
    public List<DistrictMapCenter> districtMapCenters() {
        return parkingListingService.districtMapCenters();
    }

    @ModelAttribute("today")
    public LocalDate today() {
        return LocalDate.now();
    }

    @ModelAttribute("isAuthenticated")
    public boolean isAuthenticated(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }

    @ModelAttribute("currentUserEmail")
    public String currentUserEmail(Authentication authentication) {
        if (authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            return authentication.getName();
        }
        return null;
    }

    @ModelAttribute("districtNames")
    public Map<District, String> districtNames() {
        return Map.ofEntries(
                Map.entry(District.CENTER, District.CENTER.getDisplayName()),
                Map.entry(District.LOZENETS, District.LOZENETS.getDisplayName()),
                Map.entry(District.MLADOST, District.MLADOST.getDisplayName()),
                Map.entry(District.STUDENTSKI, District.STUDENTSKI.getDisplayName()),
                Map.entry(District.LYULIN, District.LYULIN.getDisplayName()),
                Map.entry(District.OBORISHTE, District.OBORISHTE.getDisplayName()),
                Map.entry(District.PODUYANE, District.PODUYANE.getDisplayName()),
                Map.entry(District.KRASNO_SELO, District.KRASNO_SELO.getDisplayName()),
                Map.entry(District.GEO_MILEV, District.GEO_MILEV.getDisplayName()),
                Map.entry(District.MANASTIRSKI_LIVADI, District.MANASTIRSKI_LIVADI.getDisplayName()),
                Map.entry(District.IVAN_VAZOV, District.IVAN_VAZOV.getDisplayName()),
                Map.entry(District.BOROVO, District.BOROVO.getDisplayName()),
                Map.entry(District.DIANABAD, District.DIANABAD.getDisplayName()),
                Map.entry(District.DRUZHBA, District.DRUZHBA.getDisplayName()),
                Map.entry(District.NADEZHDA, District.NADEZHDA.getDisplayName()),
                Map.entry(District.BANISHORA, District.BANISHORA.getDisplayName()),
                Map.entry(District.OVCHA_KUPEL, District.OVCHA_KUPEL.getDisplayName()),
                Map.entry(District.GOTSE_DELCHEV, District.GOTSE_DELCHEV.getDisplayName()),
                Map.entry(District.IZTOK, District.IZTOK.getDisplayName()),
                Map.entry(District.SVETA_TROITSA, District.SVETA_TROITSA.getDisplayName()),
                Map.entry(District.OTHER, District.OTHER.getDisplayName())
        );
    }
}
