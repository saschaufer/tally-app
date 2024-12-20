package de.saschaufer.tallyapp.controller.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PostCreatePaymentRequest(

        @NotNull(message = "Payment amount is required")
        BigDecimal amount
) {
}
