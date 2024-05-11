package de.saschaufer.apps.tally.config.security;

import de.saschaufer.apps.tally.controller.Handler;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.server.ServerRequest;

import java.time.Instant;

import static de.saschaufer.apps.tally.persistence.dto.User.Role.USER;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

class SecurityConfigTest extends SecurityConfigSetup {

    @MockBean
    Handler handler;

    @Test
    void root_positive_IndexHtml() {

        webClient.get().uri("/")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(startsWith("<!doctype html>"));
    }

    @Test
    void getIndexHtml_negative_HtmlBlocked() {

        webClient.get().uri("/index.html")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody().isEmpty();
    }

    @Test
    void getJs_positive() {

        webClient.get().uri("/script.js")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(startsWith("code"));
    }

    @Test
    void getCss_positive() {

        webClient.get().uri("/styles.css")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(startsWith("style"));
    }

    @Test
    void getIco_positive() {

        webClient.get().uri("/icon.ico")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(startsWith("icon"));
    }

    @Test
    void postLogin_positive_Password() {

        doReturn(ok().bodyValue("jwt")).when(handler).postLogin(any(ServerRequest.class));

        webClient.post().uri("/login")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + credentials(USER, true))
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo("jwt");

        verify(handler, times(1)).postLogin(any(ServerRequest.class));
    }

    @Test
    void postLogin_negative_UserWrong() {

        webClient.post().uri("/login")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + credentials(USER + "-wrong", false))
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody().isEmpty();

        verify(handler, times(0)).postLogin(any(ServerRequest.class));
    }

    @Test
    void postLogin_negative_PasswordWrong() {

        webClient.post().uri("/login")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + credentials(USER, false))
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody().isEmpty();

        verify(handler, times(0)).postLogin(any(ServerRequest.class));
    }

    @Test
    void postLogin_positive_Jwt() {

        doReturn(ok().bodyValue("jwt")).when(handler).postLogin(any(ServerRequest.class));

        webClient.post().uri("/login")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + testJwt(USER, Instant.now()))
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo("jwt");

        verify(handler, times(1)).postLogin(any(ServerRequest.class));
    }

    @Test
    void postLogin_negative_JwtExpired() {

        webClient.post().uri("/login")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + testJwt(USER, Instant.now().minusSeconds(100000)))
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody().isEmpty();

        verify(handler, times(0)).postLogin(any(ServerRequest.class));
    }

    @Test
    void postLogin_negative_JwtUserWrong() {

        webClient.post().uri("/login")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + testJwt(USER + "-wrong", Instant.now().minusSeconds(100000)))
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody().isEmpty();

        verify(handler, times(0)).postLogin(any(ServerRequest.class));
    }
}
