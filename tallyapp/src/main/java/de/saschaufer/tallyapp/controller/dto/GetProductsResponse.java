package de.saschaufer.tallyapp.controller.dto;

import java.math.BigDecimal;

public record GetProductsResponse(
        Long id,
        String name,
        BigDecimal price
) {
}
