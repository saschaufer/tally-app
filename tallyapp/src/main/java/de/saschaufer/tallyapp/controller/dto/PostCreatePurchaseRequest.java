package de.saschaufer.tallyapp.controller.dto;

import jakarta.validation.constraints.NotNull;

public record PostCreatePurchaseRequest(

        @NotNull(message = "Product ID is required")
        Long productId
) {
}
