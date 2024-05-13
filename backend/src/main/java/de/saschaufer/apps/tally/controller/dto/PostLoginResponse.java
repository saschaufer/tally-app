package de.saschaufer.apps.tally.controller.dto;

public record PostLoginResponse(
        String jwt,
        Boolean secure
) {
}
