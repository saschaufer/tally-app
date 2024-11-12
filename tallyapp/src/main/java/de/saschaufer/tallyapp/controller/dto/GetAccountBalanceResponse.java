package de.saschaufer.tallyapp.controller.dto;

import java.math.BigDecimal;

public record GetAccountBalanceResponse(
        BigDecimal amountPayments,
        BigDecimal amountPurchases,
        BigDecimal amountTotal
) {
}
