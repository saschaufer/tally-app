package de.saschaufer.tallyapp.services;

import de.saschaufer.tallyapp.config.currency.CurrencyProperties;
import de.saschaufer.tallyapp.controller.dto.PostLoginResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FrontendPropertiesService {

    private final CurrencyProperties currencyProperties;

    public PostLoginResponse addFrontendProperties(final PostLoginResponse response) {

        final PostLoginResponse.Properties properties = new PostLoginResponse.Properties(
                currencyProperties.symbol()
        );

        return new PostLoginResponse(response.jwt(), response.secure(), properties);
    }
}
