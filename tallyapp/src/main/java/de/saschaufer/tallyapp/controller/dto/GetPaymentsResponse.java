package de.saschaufer.tallyapp.controller.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record GetPaymentsResponse(
        Long id,
        BigDecimal amount,
        Instant timestamp
) {
}
