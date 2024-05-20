package de.saschaufer.apps.tally.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PostUpdateProductRequest(

        @NotNull(message = "Product ID is required")
        Long id,

        @NotBlank(message = "Product name is required")
        String name
) {
}
