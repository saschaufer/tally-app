package de.saschaufer.apps.tally.config.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.time.DurationMax;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties("jwt")
public record JwtProperties(

        @NotBlank
        String issuer,

        @NotBlank
        String audience,

        @NotNull
        @DurationMin(minutes = 10)
        @DurationMax(hours = 24)
        Duration expirationTime,

        @NotBlank
        @Size(min = 31)
        String key,

        @NotNull
        Boolean secure
) {
}
