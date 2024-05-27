package de.saschaufer.apps.tally.controller.dto;

import jakarta.validation.constraints.NotNull;

public record PostCreatePurchaseRequest(

        @NotNull(message = "Product ID is required")
        Long productId
) {
}
