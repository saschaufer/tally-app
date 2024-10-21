package de.saschaufer.apps.tally.controller.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record GetUsersResponse(
        String email,
        LocalDateTime registrationOn,
        boolean registrationComplete,
        List<String> roles,
        BigDecimal accountBalance
) {
}
