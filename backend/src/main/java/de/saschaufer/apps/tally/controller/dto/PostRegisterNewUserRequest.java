package de.saschaufer.apps.tally.controller.dto;

import jakarta.validation.GroupSequence;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

interface First {
}

interface Second {
}

@GroupSequence({PostRegisterNewUserRequest.class, First.class, Second.class})
public record PostRegisterNewUserRequest(

        @NotBlank(message = "Email is required", groups = First.class)
        @Email(message = "Must be a valid email address", groups = Second.class)
        String email,

        @NotBlank(message = "Password is required")
        String password
) {
}
