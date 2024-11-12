package de.saschaufer.tallyapp.config.db;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("database")
public record DbProperties(
        @NotBlank
        String url
) {
}
