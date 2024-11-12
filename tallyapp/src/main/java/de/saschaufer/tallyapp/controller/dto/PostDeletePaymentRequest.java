package de.saschaufer.tallyapp.controller.dto;

import jakarta.validation.constraints.NotNull;

public record PostDeletePaymentRequest(

        @NotNull(message = "Payment ID is required")
        Long paymentId
) {
}
