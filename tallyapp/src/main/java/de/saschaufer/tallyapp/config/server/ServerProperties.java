package de.saschaufer.tallyapp.config.server;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("server")
public record ServerProperties(
        @NotNull
        @Min(0)
        @Max(65535)
        Integer port
) {
}
