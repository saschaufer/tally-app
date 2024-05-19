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
    void postChangePassword_positive_User() {

        doReturn(Mono.just(new User())).when(userDetailsService).changePassword(any(User.class), any(String.class));

        webClient.post().uri("/settings/change-password")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + credentials(USER, true))
                .body(Mono.just("new-password"), String.class)
                .exchange()
                .expectStatus().isOk()
                .expectBody().isEmpty();

        verify(userDetailsService, times(2)).findByUsername(USER);
        verify(userDetailsService, times(1)).changePassword(any(User.class), any(String.class));

        final ArgumentCaptor<User> argumentCaptorUser = ArgumentCaptor.forClass(User.class);
        final ArgumentCaptor<String> argumentCaptorNewPassword = ArgumentCaptor.forClass(String.class);

        verify(userDetailsService).changePassword(argumentCaptorUser.capture(), argumentCaptorNewPassword.capture());

        final User user = argumentCaptorUser.getValue();
        assertThat(user.getUsername(), is(USER));
        assertThat(user.getPassword(), is(ENCODED_PASSWORD));
        assertThat(user.getRoles(), is(USER));

        assertThat(argumentCaptorNewPassword.getValue(), is("new-password"));
    }

    @Test
    void postChangePassword_positive_Jwt() {

        doReturn(Mono.just(new User())).when(userDetailsService).changePassword(any(User.class), any(String.class));

        webClient.post().uri("/settings/change-password")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + testJwt(USER))
                .body(Mono.just("new-password"), String.class)
                .exchange()
                .expectStatus().isOk()
                .expectBody().isEmpty();

        verify(userDetailsService, times(1)).findByUsername(USER);
        verify(userDetailsService, times(1)).changePassword(any(User.class), any(String.class));

        final ArgumentCaptor<User> argumentCaptorUser = ArgumentCaptor.forClass(User.class);
        final ArgumentCaptor<String> argumentCaptorNewPassword = ArgumentCaptor.forClass(String.class);

        verify(userDetailsService).changePassword(argumentCaptorUser.capture(), argumentCaptorNewPassword.capture());

        final User user = argumentCaptorUser.getValue();
        assertThat(user.getUsername(), is(USER));
        assertThat(user.getPassword(), is(ENCODED_PASSWORD));
        assertThat(user.getRoles(), is(USER));

        assertThat(argumentCaptorNewPassword.getValue(), is("new-password"));
    }

    @Test
    void postChangePassword_negative_NoBody() {

        webClient.post().uri("/settings/change-password")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + credentials(USER, true))
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentType(MediaType.TEXT_PLAIN)
                .expectBody(String.class).isEqualTo("Body required");

        verify(userDetailsService, times(2)).findByUsername(USER);
        verify(userDetailsService, times(0)).changePassword(any(User.class), any(String.class));
    }

    @Test
    void postChangePassword_negative_InternalServerError() {

        doReturn(Mono.error(new RuntimeException("Bad"))).when(userDetailsService).changePassword(any(User.class), any(String.class));

        webClient.post().uri("/settings/change-password")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + credentials(USER, true))
                .body(Mono.just("new-password"), String.class)
                .exchange()
                .expectStatus().is5xxServerError()
                .expectBody().isEmpty();

        verify(userDetailsService, times(2)).findByUsername(USER);
        verify(userDetailsService, times(1)).changePassword(any(User.class), any(String.class));
    }
}
