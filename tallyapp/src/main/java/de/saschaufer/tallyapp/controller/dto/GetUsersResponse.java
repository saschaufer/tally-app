package de.saschaufer.tallyapp.controller.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record GetUsersResponse(
        String email,
        Instant registrationOn,
        boolean registrationComplete,
        List<String> roles,
        BigDecimal accountBalance
) {
}
