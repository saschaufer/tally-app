package de.saschaufer.apps.tally.controller.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record GetPaymentsResponse(
        Long id,
        BigDecimal amount,
        LocalDateTime timestamp
) {
}
