package de.saschaufer.tallyapp.controller.dto;

import jakarta.validation.constraints.NotNull;

public record PostReadProductRequest(

        @NotNull(message = "Product ID is required")
        Long id
) {
}
