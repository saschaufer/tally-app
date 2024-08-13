package de.saschaufer.apps.tally.controller.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record PostRegisterNewUserConfirmRequest(

        @NotBlank(message = "Email is required")
        @Email(message = "Must be a valid email address")
        String email,

        @NotBlank(message = "Registration secret is required")
        String registrationSecret
) {
}
