package de.saschaufer.tallyapp.controller.dto;

public record PostLoginResponse(
        String jwt,
        Boolean secure
) {
}
