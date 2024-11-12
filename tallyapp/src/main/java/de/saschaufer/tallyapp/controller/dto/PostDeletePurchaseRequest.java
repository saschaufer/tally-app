package de.saschaufer.tallyapp.controller.dto;

import jakarta.validation.constraints.NotNull;

public record PostDeletePurchaseRequest(

        @NotNull(message = "Purchase ID is required")
        Long purchaseId
) {
}
