package de.saschaufer.apps.tally.config.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("jwt")
public record JwtProperties(

        @NotBlank
        String issuer,

        @NotBlank
        String audience,

        @NotBlank
        @Size(min = 31)
        String key
) {
}
