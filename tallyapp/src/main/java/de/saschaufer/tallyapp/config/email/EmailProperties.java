package de.saschaufer.tallyapp.config.email;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.time.DurationMax;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties("email")
public record EmailProperties(

        @NotBlank
        @Email
        String from,

        @NotBlank
        String registrationUrl,

        @NotNull
        @DurationMin(minutes = 10)
        @DurationMax(hours = 24)
        Duration deleteUnregisteredUsersAfter
) {
}
