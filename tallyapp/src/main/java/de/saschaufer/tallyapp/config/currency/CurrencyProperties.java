package de.saschaufer.tallyapp.config.currency;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("currency")
public record CurrencyProperties(

        @NotBlank
        String symbol
) {
}
