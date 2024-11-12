package de.saschaufer.tallyapp.controller.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PostUpdateProductPriceRequest(

        @NotNull(message = "Product ID is required")
        Long id,

        @NotNull(message = "Product price is required")
        BigDecimal price
) {
}
