package de.saschaufer.apps.tally.controller.dto;

import jakarta.validation.constraints.NotNull;

public record PostDeletePurchaseRequest(

        @NotNull(message = "Purchase ID is required")
        Long purchaseId
) {
}
