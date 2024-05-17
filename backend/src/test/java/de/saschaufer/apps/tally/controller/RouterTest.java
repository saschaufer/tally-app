package de.saschaufer.apps.tally.controller;

import de.saschaufer.apps.tally.config.security.SecurityConfigSetup;
import de.saschaufer.apps.tally.controller.dto.PostLoginResponse;
import de.saschaufer.apps.tally.controller.dto.PostRegisterNewUserRequest;
import de.saschaufer.apps.tally.persistence.dto.User;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.List;

import static de.saschaufer.apps.tally.persistence.dto.User.Role.INVITATION;
import static de.saschaufer.apps.tally.persistence.dto.User.Role.USER;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RouterTest extends SecurityConfigSetup {

    @Test
    void postLogin_positive() {

        doReturn(new PostLoginResponse("jwt", true)).when(userDetailsService).createJwtToken(any(User.class));

        webClient.post().uri("/login")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + credentials(USER, true))
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody(PostLoginResponse.class).isEqualTo(new PostLoginResponse("jwt", true));

        verify(userDetailsService, times(1)).findByUsername(any(String.class));
        verify(userDetailsService, times(1)).createJwtToken(any(User.class));

        final ArgumentCaptor<User> argumentCaptor = ArgumentCaptor.forClass(User.class);
        verify(userDetailsService).createJwtToken(argumentCaptor.capture());

        final User user = argumentCaptor.getValue();

        assertThat(user.getId(), is(2L));
        assertThat(user.getUsername(), is(USER));
        assertThat(user.getPassword(), notNullValue());
        assertThat(user.getRoles(), is(USER));
    }

    @Test
    void postRegisterNewUser_positive() {

        doReturn(new PostLoginResponse("jwt", true)).when(userDetailsService).createJwtToken(any(User.class));
        doReturn(Mono.just(new User())).when(userDetailsService).createUser(any(String.class), any(String.class), ArgumentMatchers.<String>anyList());

        webClient.post().uri("/register")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + credentials(INVITATION, true))
                .body(Mono.just(new PostRegisterNewUserRequest("test-user", "test-password")), PostRegisterNewUserRequest.class)
                .exchange()
                .expectStatus().isOk()
                .expectBody().isEmpty();

        verify(userDetailsService, times(1)).findByUsername(any(String.class));
        verify(userDetailsService, times(1)).createUser(any(String.class), any(String.class), ArgumentMatchers.<String>anyList());

        final ArgumentCaptor<String> argumentCaptorUsername = ArgumentCaptor.forClass(String.class);
        final ArgumentCaptor<String> argumentCaptorPassword = ArgumentCaptor.forClass(String.class);

        @SuppressWarnings("unchecked") // Can't get any better
        final ArgumentCaptor<? extends List<String>> argumentCaptorRoles = ArgumentCaptor.forClass((Class) List.class);

        verify(userDetailsService).createUser(argumentCaptorUsername.capture(), argumentCaptorPassword.capture(), argumentCaptorRoles.capture());

        assertThat(argumentCaptorUsername.getValue(), is("test-user"));
        assertThat(argumentCaptorPassword.getValue(), is("test-password"));
        assertThat(argumentCaptorRoles.getValue(), containsInAnyOrder(USER));
    }

    @Test
    void postRegisterNewUser_negative_NoBody() {

        doReturn(new PostLoginResponse("jwt", true)).when(userDetailsService).createJwtToken(any(User.class));

        webClient.post().uri("/register")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + credentials(INVITATION, true))
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentType(MediaType.TEXT_PLAIN)
                .expectBody(String.class).isEqualTo("Body required");

        verify(userDetailsService, times(1)).findByUsername(any(String.class));
        verify(userDetailsService, times(0)).createUser(any(String.class), any(String.class), ArgumentMatchers.<String>anyList());
    }

    @Test
    void postRegisterNewUser_negative_BodyWrongType() {

        doReturn(new PostLoginResponse("jwt", true)).when(userDetailsService).createJwtToken(any(User.class));

        webClient.post().uri("/register")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + credentials(INVITATION, true))
                .body(Mono.just("Wrong Type"), String.class)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .expectHeader().contentType(MediaType.TEXT_PLAIN)
                .expectBody(String.class).value(stringContainsInOrder("Content type '", "' not supported. Supported: '"));

        verify(userDetailsService, times(1)).findByUsername(any(String.class));
        verify(userDetailsService, times(0)).createUser(any(String.class), any(String.class), ArgumentMatchers.<String>anyList());
    }

    @Test
    void postRegisterNewUser_negative_Validator() {

        doReturn(new PostLoginResponse("jwt", true)).when(userDetailsService).createJwtToken(any(User.class));

        webClient.post().uri("/register")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + credentials(INVITATION, true))
                .body(Mono.just(new PostRegisterNewUserRequest("", "test-password")), PostRegisterNewUserRequest.class)
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentType(MediaType.TEXT_PLAIN)
                .expectBody(String.class).isEqualTo("Username is required");

        verify(userDetailsService, times(1)).findByUsername(any(String.class));
        verify(userDetailsService, times(0)).createUser(any(String.class), any(String.class), ArgumentMatchers.<String>anyList());
    }

    @Test
    void postRegisterNewUser_negative_InternalServerError() {

        doReturn(new PostLoginResponse("jwt", true)).when(userDetailsService).createJwtToken(any(User.class));
        doReturn(Mono.error(new RuntimeException("Bad"))).when(userDetailsService).createUser(any(String.class), any(String.class), ArgumentMatchers.<String>anyList());

        webClient.post().uri("/register")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + credentials(INVITATION, true))
                .body(Mono.just(new PostRegisterNewUserRequest("test-user", "test-password")), PostRegisterNewUserRequest.class)
                .exchange()
                .expectStatus().is5xxServerError()
                .expectBody().isEmpty();

        verify(userDetailsService, times(1)).findByUsername(any(String.class));
        verify(userDetailsService, times(1)).createUser(any(String.class), any(String.class), ArgumentMatchers.<String>anyList());
    }

    @Test
    void postRegisterNewUser_negative_ResponseStatusException() {

        doReturn(new PostLoginResponse("jwt", true)).when(userDetailsService).createJwtToken(any(User.class));
        doReturn(Mono.error(new ResponseStatusException(HttpStatus.I_AM_A_TEAPOT, "Bad"))).when(userDetailsService).createUser(any(String.class), any(String.class), ArgumentMatchers.<String>anyList());

        webClient.post().uri("/register")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + credentials(INVITATION, true))
                .body(Mono.just(new PostRegisterNewUserRequest("test-user", "test-password")), PostRegisterNewUserRequest.class)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.I_AM_A_TEAPOT)
                .expectBody(String.class).isEqualTo("Bad");

        verify(userDetailsService, times(1)).findByUsername(any(String.class));
        verify(userDetailsService, times(1)).createUser(any(String.class), any(String.class), ArgumentMatchers.<String>anyList());
    }

    @Test
    void placeholder() {

        webClient.get().uri("/none")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + credentials(USER, true))
                .exchange()
                .expectStatus().isForbidden()
                .expectBody().isEmpty();

        webClient.get().uri("/user")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + credentials(USER, true))
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo("user");

        webClient.get().uri("/admin")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + credentials(USER, true))
                .exchange()
                .expectStatus().isForbidden()
                .expectBody().isEmpty();

        webClient.get().uri("/user-admin")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + credentials(USER, true))
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo("useradmin");
    }
}
