package de.saschaufer.tallyapp.controller.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record GetPurchasesResponse(
        Long purchaseId,
        Instant purchaseTimestamp,
        String productName,
        BigDecimal productPrice
) {
}
