package com.parknet.controller;

import com.parknet.dto.RegisterRequest;
import com.parknet.service.UserAccountService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private final UserAccountService userAccountService;

    public AuthController(UserAccountService userAccountService) {
        this.userAccountService = userAccountService;
    }

    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerForm(@ModelAttribute("registerRequest") RegisterRequest registerRequest) {
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(
            @Valid @ModelAttribute("registerRequest") RegisterRequest registerRequest,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            return "auth/register";
        }

        try {
            userAccountService.register(registerRequest);
        } catch (IllegalArgumentException ex) {
            String fieldName = ex.getMessage().contains("Паролите") ? "confirmPassword" : "email";
            bindingResult.addError(new FieldError(
                    "registerRequest",
                    fieldName,
                    fieldName.equals("confirmPassword") ? registerRequest.getConfirmPassword() : registerRequest.getEmail(),
                    false,
                    null,
                    null,
                    ex.getMessage()
            ));
            return "auth/register";
        }

        redirectAttributes.addFlashAttribute("registrationSuccess", "Регистрацията е успешна. Влезте с новия си профил.");
        return "redirect:/login";
    }
}
