package de.saschaufer.apps.tally.controller.dto;

import jakarta.validation.constraints.NotNull;

public record PostReadProductRequest(

        @NotNull(message = "Product ID is required")
        Long id
) {
}
