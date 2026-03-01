package de.saschaufer.tallyapp.services;

import de.saschaufer.tallyapp.config.currency.CurrencyProperties;
import de.saschaufer.tallyapp.controller.dto.PostLoginResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

class FrontendPropertiesServiceTest {

    private CurrencyProperties currencyProperties;
    private FrontendPropertiesService frontendPropertiesService;

    @BeforeEach
    void beforeEach() {
        currencyProperties = mock(CurrencyProperties.class);
        frontendPropertiesService = new FrontendPropertiesService(currencyProperties);
    }

    @Test
    void addFrontendProperties_positive_AddProperties() {

        doReturn("€").when(currencyProperties).symbol();

        final PostLoginResponse in = new PostLoginResponse("jwt", true, null);

        final PostLoginResponse out = frontendPropertiesService.addFrontendProperties(in);

        assertThat(out, is(new PostLoginResponse("jwt", true, new PostLoginResponse.Properties("€"))));
    }
}
