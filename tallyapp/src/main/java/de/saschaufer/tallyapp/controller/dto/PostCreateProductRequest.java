package de.saschaufer.tallyapp.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PostCreateProductRequest(

        @NotBlank(message = "Product name is required")
        String name,

        @NotNull(message = "Product price is required")
        BigDecimal price
) {
}
