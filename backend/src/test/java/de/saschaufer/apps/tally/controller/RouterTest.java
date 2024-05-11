package de.saschaufer.apps.tally.controller;

import de.saschaufer.apps.tally.config.security.SecurityConfigSetup;
import de.saschaufer.apps.tally.persistence.dto.User;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import static de.saschaufer.apps.tally.persistence.dto.User.Role.USER;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RouterTest extends SecurityConfigSetup {

    @Test
    void postLogin_positive() {

        doReturn("jwt").when(userDetailsService).createJwtToken(any(User.class));

        webClient.post().uri("/login")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + credentials(USER, true))
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo("jwt");

        verify(userDetailsService, times(1)).findByUsername(any(String.class));
        verify(userDetailsService, times(1)).createJwtToken(any(User.class));
    }
}
