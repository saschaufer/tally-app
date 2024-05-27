package de.saschaufer.apps.tally.controller.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record GetPurchasesResponse(
        Long purchaseId,
        LocalDateTime purchaseTimestamp,
        String productName,
        BigDecimal productPrice
) {
}
