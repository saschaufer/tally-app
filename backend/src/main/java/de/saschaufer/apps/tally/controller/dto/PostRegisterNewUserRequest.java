package de.saschaufer.apps.tally.controller.dto;

import jakarta.validation.constraints.NotBlank;

public record PostRegisterNewUserRequest(

        @NotBlank(message = "Username is required")
        String username,

        @NotBlank(message = "Password is required")
        String password
) {
}
