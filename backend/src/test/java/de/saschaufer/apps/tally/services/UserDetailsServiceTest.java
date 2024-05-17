package de.saschaufer.apps.tally.services;

import de.saschaufer.apps.tally.config.security.JwtProperties;
import de.saschaufer.apps.tally.controller.dto.PostLoginResponse;
import de.saschaufer.apps.tally.management.UserAgent;
import de.saschaufer.apps.tally.persistence.Persistence;
import de.saschaufer.apps.tally.persistence.dto.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserDetailsServiceTest {

    private Persistence persistence;
    private JwtProperties jwtProperties;
    private JwtEncoder jwtEncoder;
    private UserAgent userAgent;
    private PasswordEncoder passwordEncoder;
    private UserDetailsService userDetailsService;

    @BeforeEach
    void beforeEach() {
        persistence = mock(Persistence.class);
        jwtProperties = mock(JwtProperties.class);
        jwtEncoder = mock(JwtEncoder.class);
        userAgent = mock(UserAgent.class);
        passwordEncoder = mock(PasswordEncoder.class);
        userDetailsService = new UserDetailsService(persistence, jwtProperties, jwtEncoder, userAgent, passwordEncoder);
    }

    @Test
    void findByUsername_positive_UserExists() {

        final UserDetails userDetails = new User(1L, "username", "password", "roles");

        doReturn(Mono.just(userDetails)).when(persistence).selectUser(any(String.class));

        Mono.just("")
                .flatMap(userDetailsService::findByUsername)
                .as(StepVerifier::create)
                .assertNext(userD -> {
                    assertThat(userD.getUsername(), is(userDetails.getUsername()));
                    assertThat(userD.getPassword(), is(userDetails.getPassword()));
                })
                .verifyComplete();

        verify(persistence, times(1)).selectUser(any(String.class));
    }

    @Test
    void findByUsername_negative_UserNotExists() {

        doReturn(Mono.error(new RuntimeException("User not found"))).when(persistence).selectUser(any(String.class));

        Mono.just("")
                .flatMap(userDetailsService::findByUsername)
                .as(StepVerifier::create)
                .verifyErrorSatisfies(error -> {
                    assertThat(error, instanceOf(BadCredentialsException.class));
                    assertThat(error.getMessage(), containsString("Error finding user"));
                });

        verify(persistence, times(1)).selectUser(any(String.class));
    }

    @Test
    void updatePassword_positive_UserExists() {

        final UserDetails userDetails = new User(1L, "username", "password", "roles");

        doReturn(Mono.just(userDetails)).when(persistence).selectUser(any(String.class));
        doReturn(Mono.empty()).when(persistence).updateUserPassword(any(Long.class), any(String.class));

        Mono.just(userDetails)
                .flatMap(userD -> userDetailsService.updatePassword(userD, "newPassword"))
                .as(StepVerifier::create)
                .assertNext(userD -> {
                    assertThat(userD.getUsername(), is(userDetails.getUsername()));
                    assertThat(userD.getPassword(), is("newPassword"));
                })
                .verifyComplete();

        verify(persistence, times(1)).selectUser(any(String.class));
        verify(persistence, times(1)).updateUserPassword(any(Long.class), any(String.class));
    }

    @Test
    void updatePassword_negative_UserNotExists() {

        final UserDetails userDetails = new User(1L, "username", "password", "roles");

        doReturn(Mono.error(new RuntimeException("User not found"))).when(persistence).selectUser(any(String.class));

        Mono.just(userDetails)
                .flatMap(userD -> userDetailsService.updatePassword(userD, "newPassword"))
                .as(StepVerifier::create)
                .verifyErrorSatisfies(error ->
                        assertThat(error.getMessage(), containsString("User not found"))
                );

        verify(persistence, times(1)).selectUser(any(String.class));
        verify(persistence, times(0)).updateUserPassword(any(Long.class), any(String.class));
    }

    @Test
    void updatePassword_negative_UserNotUpdated() {

        final UserDetails userDetails = new User(1L, "username", "password", "roles");

        doReturn(Mono.just(userDetails)).when(persistence).selectUser(any(String.class));
        doReturn(Mono.error(new RuntimeException("User not updated"))).when(persistence).updateUserPassword(any(Long.class), any(String.class));

        Mono.just(userDetails)
                .flatMap(userD -> userDetailsService.updatePassword(userD, "newPassword"))
                .as(StepVerifier::create)
                .verifyErrorSatisfies(error ->
                        assertThat(error.getMessage(), containsString("User not updated"))
                );

        verify(persistence, times(1)).selectUser(any(String.class));
        verify(persistence, times(1)).updateUserPassword(any(Long.class), any(String.class));
    }

    @Test
    void createJwtToken_positive_FromProperties() {

        final User user = new User(1L, "username", "password", User.Role.USER);

        doReturn("issuer").when(jwtProperties).issuer();
        doReturn("audience").when(jwtProperties).audience();

        doReturn(new Jwt("ecoded-jwt", Instant.MIN, Instant.MAX, Map.of("header", "h"), Map.of("claim", "m"))).when(jwtEncoder).encode(any());

        final PostLoginResponse response = userDetailsService.createJwtToken(user);

        verify(jwtEncoder, times(1)).encode(any());

        assertThat(response.jwt(), is("ecoded-jwt"));
        assertThat(response.secure(), is(jwtProperties.secure()));
    }

    @Test
    void createJwtToken_positive_FromUserAgent() {

        final User user = new User(1L, "username", "password", User.Role.USER);

        doReturn("host").when(userAgent).getHostName();
        doReturn("app").when(userAgent).getAppName();

        doReturn(new Jwt("ecoded-jwt", Instant.MIN, Instant.MAX, Map.of("header", "h"), Map.of("claim", "m"))).when(jwtEncoder).encode(any());

        final PostLoginResponse response = userDetailsService.createJwtToken(user);

        verify(jwtEncoder, times(1)).encode(any());

        assertThat(response.jwt(), is("ecoded-jwt"));
        assertThat(response.secure(), is(jwtProperties.secure()));
    }

    @Test
    void createUser_positive_UserCreated() {

        doReturn(Mono.just(false)).when(persistence).existsUser(any(String.class));
        doReturn(Mono.just(new User() {{
            setId(1L);
        }})).when(persistence).insertUser(any(User.class));

        doReturn("encoded-password").when(passwordEncoder).encode(any(String.class));

        userDetailsService.createUser("new-user", "test-password", List.of("a", "b", "c"))
                .as(StepVerifier::create)
                .assertNext(user -> {
                    assertThat(user.getId(), is(1L));
                    assertThat(user.getPassword(), nullValue());
                })
                .verifyComplete();

        verify(persistence, times(1)).existsUser(any(String.class));
        verify(persistence, times(1)).insertUser(any(User.class));

        final ArgumentCaptor<String> argumentCaptorExists = ArgumentCaptor.forClass(String.class);
        verify(persistence).existsUser(argumentCaptorExists.capture());

        final ArgumentCaptor<User> argumentCaptorInsert = ArgumentCaptor.forClass(User.class);
        verify(persistence).insertUser(argumentCaptorInsert.capture());

        assertThat(argumentCaptorExists.getValue(), is("new-user"));

        final User user = argumentCaptorInsert.getValue();

        assertThat(user.getId(), nullValue());
        assertThat(user.getUsername(), is("new-user"));
        assertThat(user.getPassword(), is("encoded-password"));
        assertThat(user.getRoles(), is(String.join(",", List.of("a", "b", "c"))));
    }

    @Test
    void createUser_positive_UsernameTaken() {

        doReturn(Mono.just(true)).when(persistence).existsUser(any(String.class));

        userDetailsService.createUser("new-user", "test-password", List.of("a", "b", "c"))
                .as(StepVerifier::create)
                .verifyErrorSatisfies(error -> {
                    assertThat(error, instanceOf(ResponseStatusException.class));

                    final ResponseStatusException e = (ResponseStatusException) error;
                    assertThat(e.getStatusCode(), is(HttpStatus.BAD_REQUEST));
                    assertThat(e.getReason(), is("Username is taken"));
                });

        verify(persistence, times(1)).existsUser(any(String.class));
        verify(persistence, times(0)).insertUser(any(User.class));

        final ArgumentCaptor<String> argumentCaptorExists = ArgumentCaptor.forClass(String.class);
        verify(persistence).existsUser(argumentCaptorExists.capture());

        assertThat(argumentCaptorExists.getValue(), is("new-user"));
    }

    @Test
    void createAdminIfNotExists_positive_UserNotExists() {

        doReturn(Mono.just(false)).when(persistence).existsUser(any(String.class));
        doReturn(Mono.just(new User())).when(persistence).insertUser(any(User.class));

        doReturn("encoded-password").when(passwordEncoder).encode(any(String.class));

        userDetailsService.createAdminIfNotExists();

        verify(persistence, times(1)).existsUser(any(String.class));
        verify(persistence, times(1)).insertUser(any(User.class));

        final ArgumentCaptor<String> argumentCaptorExists = ArgumentCaptor.forClass(String.class);
        verify(persistence).existsUser(argumentCaptorExists.capture());

        final ArgumentCaptor<User> argumentCaptorInsert = ArgumentCaptor.forClass(User.class);
        verify(persistence).insertUser(argumentCaptorInsert.capture());

        assertThat(argumentCaptorExists.getValue(), is("admin"));

        final User user = argumentCaptorInsert.getValue();

        assertThat(user.getId(), nullValue());
        assertThat(user.getUsername(), is("admin"));
        assertThat(user.getPassword(), is("encoded-password"));
        assertThat(user.getRoles(), is(String.join(",", User.Role.USER, User.Role.ADMIN)));
    }

    @Test
    void createAdminIfNotExists_positive_UserExists() {

        doReturn(Mono.just(true)).when(persistence).existsUser(any(String.class));

        userDetailsService.createAdminIfNotExists();

        verify(persistence, times(1)).existsUser(any(String.class));
        verify(persistence, times(0)).insertUser(any(User.class));

        final ArgumentCaptor<String> argumentCaptorExists = ArgumentCaptor.forClass(String.class);
        verify(persistence).existsUser(argumentCaptorExists.capture());

        assertThat(argumentCaptorExists.getValue(), is("admin"));
    }

    @Test
    void createInvitationCodeIfNotExists_positive_InvitationNotExists() {

        doReturn(Mono.just(false)).when(persistence).existsUser(any(String.class));
        doReturn(Mono.just(new User())).when(persistence).insertUser(any(User.class));

        doReturn("encoded-invitation-code").when(passwordEncoder).encode(any(String.class));

        userDetailsService.createInvitationCodeIfNoneExists();

        verify(persistence, times(1)).existsUser(any(String.class));
        verify(persistence, times(1)).insertUser(any(User.class));

        final ArgumentCaptor<String> argumentCaptorExists = ArgumentCaptor.forClass(String.class);
        verify(persistence).existsUser(argumentCaptorExists.capture());

        final ArgumentCaptor<User> argumentCaptorInsert = ArgumentCaptor.forClass(User.class);
        verify(persistence).insertUser(argumentCaptorInsert.capture());

        assertThat(argumentCaptorExists.getValue(), is("invitation-code"));

        final User user = argumentCaptorInsert.getValue();

        assertThat(user.getId(), nullValue());
        assertThat(user.getUsername(), is("invitation-code"));
        assertThat(user.getPassword(), is("encoded-invitation-code"));
        assertThat(user.getRoles(), is(User.Role.INVITATION));
    }

    @Test
    void createInvitationCodeIfNotExists_positive_InvitationCodeExists() {

        doReturn(Mono.just(true)).when(persistence).existsUser(any(String.class));

        userDetailsService.createInvitationCodeIfNoneExists();

        verify(persistence, times(1)).existsUser(any(String.class));
        verify(persistence, times(0)).insertUser(any(User.class));

        final ArgumentCaptor<String> argumentCaptorExists = ArgumentCaptor.forClass(String.class);
        verify(persistence).existsUser(argumentCaptorExists.capture());

        assertThat(argumentCaptorExists.getValue(), is("invitation-code"));
    }
}
