package de.saschaufer.tallyapp.config.admin;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
@ConfigurationProperties("admin")
public record AdminProperties(
        @NotNull
        List<@NotBlank @Email String> emails
) {
}
