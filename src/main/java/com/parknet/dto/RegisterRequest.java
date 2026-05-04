package com.parknet.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RegisterRequest {

    @NotBlank(message = "Въведете име.")
    @Size(max = 120, message = "Името трябва да е до 120 символа.")
    private String fullName;

    @NotBlank(message = "Въведете имейл.")
    @Email(message = "Въведете валиден имейл.")
    @Size(max = 160, message = "Имейлът трябва да е до 160 символа.")
    private String email;

    @NotBlank(message = "Въведете парола.")
    @Size(min = 6, max = 100, message = "Паролата трябва да е поне 6 символа.")
    private String password;

    @NotBlank(message = "Повторете паролата.")
    @Size(min = 6, max = 100, message = "Повторената парола трябва да е поне 6 символа.")
    private String confirmPassword;

    public RegisterRequest() {
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }
}
