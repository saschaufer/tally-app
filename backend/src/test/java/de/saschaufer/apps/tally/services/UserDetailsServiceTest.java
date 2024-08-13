package de.saschaufer.apps.tally.services;

import de.saschaufer.apps.tally.config.admin.AdminProperties;
import de.saschaufer.apps.tally.config.email.EmailProperties;
import de.saschaufer.apps.tally.config.security.JwtProperties;
import de.saschaufer.apps.tally.controller.dto.PostLoginResponse;
import de.saschaufer.apps.tally.management.UserAgent;
import de.saschaufer.apps.tally.persistence.Persistence;
import de.saschaufer.apps.tally.persistence.dto.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.util.Pair;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserDetailsServiceTest {

    private Persistence persistence;
    private JwtProperties jwtProperties;
    private AdminProperties adminProperties;
    private JwtEncoder jwtEncoder;
    private UserAgent userAgent;
    private PasswordEncoder passwordEncoder;
    private UserDetailsService userDetailsService;

    @BeforeEach
    void beforeEach() {
        persistence = mock(Persistence.class);
        jwtProperties = mock(JwtProperties.class);
        adminProperties = mock(AdminProperties.class);
        jwtEncoder = mock(JwtEncoder.class);
        userAgent = mock(UserAgent.class);
        passwordEncoder = mock(PasswordEncoder.class);
        userDetailsService = new UserDetailsService(persistence, jwtProperties, adminProperties, mock(EmailProperties.class), jwtEncoder, userAgent, passwordEncoder);
    }

    @Test
    void findByUsername_positive_UserExists() {

        final UserDetails userDetails = new User(1L, "username@mail.com", "password", "roles", "registration-secret", LocalDateTime.of(2024, 5, 19, 23, 54, 1), true);

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
    void checkRegistered_positive_RegistrationCompleteTrue() {

        final User user = new User();
        user.setRegistrationComplete(true);

        Mono.just(user)
                .flatMap(userDetailsService::checkRegistered)
                .as(StepVerifier::create)
                .assertNext(userD -> {
                    assertThat(userD.getUsername(), is(user.getUsername()));
                    assertThat(userD.getPassword(), is(user.getPassword()));
                })
                .verifyComplete();
    }

    @Test
    void checkRegistered_negative_RegistrationCompleteFalse() {

        final User user = new User();
        user.setRegistrationComplete(false);

        Mono.just(user)
                .flatMap(userDetailsService::checkRegistered)
                .as(StepVerifier::create)
                .verifyErrorSatisfies(error -> {
                    assertThat(error, instanceOf(ResponseStatusException.class));
                    assertThat(error.getMessage(), containsString("Registration is not completed"));

                    final ResponseStatusException e = (ResponseStatusException) error;
                    assertThat(e.getStatusCode(), is(HttpStatus.UNAUTHORIZED));
                });
    }

    @Test
    void updatePassword_positive_UserExists() {

        final UserDetails userDetails = new User(1L, "username@mail.com", "password", "roles", "registration-secret", LocalDateTime.of(2024, 5, 19, 23, 54, 1), true);

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

        final UserDetails userDetails = new User(1L, "username@mail.com", "password", "roles", "registration-secret", LocalDateTime.of(2024, 5, 19, 23, 54, 1), true);

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

        final UserDetails userDetails = new User(1L, "username@mail.com", "password", "roles", "registration-secret", LocalDateTime.of(2024, 5, 19, 23, 54, 1), true);

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
    void changePassword_positive() {

        final UserDetails userDetails = new User(1L, "username-1@mail.com", null, null, null, null, null);
        final User user = new User(null, "username-2@mail.com", null, null, null, null, null);

        doReturn("encoded-password").when(passwordEncoder).encode(any(String.class));
        doReturn(Mono.just(userDetails)).when(persistence).selectUser(any(String.class));
        doReturn(Mono.empty()).when(persistence).updateUserPassword(any(Long.class), any(String.class));

        Mono.just(Pair.of(user, "password"))
                .flatMap(pair -> userDetailsService.changePassword(pair.getFirst(), pair.getSecond()))
                .as(StepVerifier::create)
                .assertNext(userD -> {
                    assertThat(userD.getUsername(), is("username-2@mail.com"));
                    assertThat(userD.getPassword(), is("encoded-password"));
                })
                .verifyComplete();

        verify(passwordEncoder, times(1)).encode("password");
        verify(persistence, times(1)).selectUser("username-2@mail.com");
        verify(persistence, times(1)).updateUserPassword(1L, "encoded-password");
    }

    @Test
    void changePassword_negative_updatePasswordFailes() {

        final User user = new User(null, "username-2@mail.com", null, null, null, null, null);

        doReturn("encoded-password").when(passwordEncoder).encode(any(String.class));
        doReturn(Mono.error(new Exception("Error"))).when(persistence).selectUser(any(String.class));

        Mono.just(Pair.of(user, "password"))
                .flatMap(pair -> userDetailsService.changePassword(pair.getFirst(), pair.getSecond()))
                .as(StepVerifier::create)
                .verifyErrorSatisfies(error ->
                        assertThat(error.getMessage(), is("Error"))
                );

        verify(passwordEncoder, times(1)).encode("password");
        verify(persistence, times(1)).selectUser("username-2@mail.com");
        verify(persistence, times(0)).updateUserPassword(any(Long.class), any(String.class));
    }

    @Test
    void createJwtToken_positive_FromProperties() {

        final User user = new User(1L, "username@mail.com", "password", User.Role.USER, null, null, null);

        doReturn("issuer").when(jwtProperties).issuer();
        doReturn("audience").when(jwtProperties).audience();

        doReturn(new Jwt("ecoded-jwt", Instant.MIN, Instant.MAX, Map.of("header", "h"), Map.of("claim", "m"))).when(jwtEncoder).encode(any());

        final PostLoginResponse response = userDetailsService.createJwtToken(user);

        verify(jwtEncoder, times(1)).encode(any());
        verify(adminProperties, times(1)).emails();

        assertThat(response.jwt(), is("ecoded-jwt"));
        assertThat(response.secure(), is(jwtProperties.secure()));
    }

    @Test
    void createJwtToken_positive_FromUserAgent() {

        final User user = new User(1L, "username@mail.com", "password", User.Role.USER, null, null, null);

        doReturn("host").when(userAgent).getHostName();
        doReturn("app").when(userAgent).getAppName();

        doReturn(new Jwt("ecoded-jwt", Instant.MIN, Instant.MAX, Map.of("header", "h"), Map.of("claim", "m"))).when(jwtEncoder).encode(any());

        final PostLoginResponse response = userDetailsService.createJwtToken(user);

        verify(jwtEncoder, times(1)).encode(any());
        verify(adminProperties, times(1)).emails();

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

        userDetailsService.createUser("new-user@mail.com", "test-password", List.of("a", "b", "c"))
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

        assertThat(argumentCaptorExists.getValue(), is("new-user@mail.com"));

        final User user = argumentCaptorInsert.getValue();

        assertThat(user.getId(), nullValue());
        assertThat(user.getUsername(), is("new-user@mail.com"));
        assertThat(user.getPassword(), is("encoded-password"));
        assertThat(user.getRoles(), is(String.join(",", List.of("a", "b", "c"))));
        assertThat(Integer.valueOf(user.getRegistrationSecret()), greaterThanOrEqualTo(16234));
        assertThat(Integer.valueOf(user.getRegistrationSecret()), lessThanOrEqualTo(97942));
        assertThat(user.getRegistrationOn().isAfter(LocalDateTime.now().minusMinutes(1)), is(true));
        assertThat(user.getRegistrationOn().isBefore(LocalDateTime.now()), is(true));
        assertThat(user.getRegistrationComplete(), is(false));
    }

    @Test
    void createUser_positive_UsernameTaken() {

        doReturn(Mono.just(true)).when(persistence).existsUser(any(String.class));

        userDetailsService.createUser("new-user@mail.com", "test-password", List.of("a", "b", "c"))
                .as(StepVerifier::create)
                .verifyErrorSatisfies(error -> {
                    assertThat(error, instanceOf(ResponseStatusException.class));

                    final ResponseStatusException e = (ResponseStatusException) error;
                    assertThat(e.getStatusCode(), is(HttpStatus.BAD_REQUEST));
                    assertThat(e.getReason(), is("Email is taken"));
                });

        verify(persistence, times(1)).existsUser(any(String.class));
        verify(persistence, times(0)).insertUser(any(User.class));

        final ArgumentCaptor<String> argumentCaptorExists = ArgumentCaptor.forClass(String.class);
        verify(persistence).existsUser(argumentCaptorExists.capture());

        assertThat(argumentCaptorExists.getValue(), is("new-user@mail.com"));
    }

    @Test
    void checkRegistrationSecret_positive() {

        final User user = new User();
        user.setRegistrationSecret("12345");

        doReturn(Mono.just(user)).when(persistence).selectUser(any(String.class));

        userDetailsService.checkRegistrationSecret("user@mail.com", "12345")
                .as(StepVerifier::create)
                .assertNext(u -> {
                    assertThat(user.getRegistrationSecret(), is("12345"));
                })
                .verifyComplete();

        verify(persistence, times(1)).selectUser("user@mail.com");
    }

    @Test
    void checkRegistrationSecret_negative_RegistrationSecretUnequal() {

        final User user = new User();
        user.setRegistrationSecret("12345");

        doReturn(Mono.just(user)).when(persistence).selectUser(any(String.class));

        userDetailsService.checkRegistrationSecret("user@mail.com", "54321")
                .as(StepVerifier::create)
                .verifyErrorSatisfies(error -> {
                    assertThat(error, instanceOf(ResponseStatusException.class));
                    assertThat(error.getMessage(), containsString("The registration secret is not correct"));

                    final ResponseStatusException e = (ResponseStatusException) error;
                    assertThat(e.getStatusCode(), is(HttpStatus.UNAUTHORIZED));
                });

        verify(persistence, times(1)).selectUser("user@mail.com");
    }

    @Test
    void updateUserRegistrationComplete_positive() {

        doReturn(Mono.empty()).when(persistence).updateUserRegistrationComplete(any(String.class));

        userDetailsService.updateUserRegistrationComplete("user@mail.com")
                .as(StepVerifier::create)
                .expectNext()
                .verifyComplete();

        verify(persistence, times(1)).updateUserRegistrationComplete("user@mail.com");
    }

    @Test
    void updateUserRegistrationComplete_negative() {

        doReturn(Mono.error(new Exception("Error"))).when(persistence).updateUserRegistrationComplete(any(String.class));

        userDetailsService.updateUserRegistrationComplete("user@mail.com")
                .as(StepVerifier::create)
                .verifyErrorSatisfies(error ->
                        assertThat(error.getMessage(), is("Error"))
                );

        verify(persistence, times(1)).updateUserRegistrationComplete("user@mail.com");
    }

    @Test
    void deleteUnregisteredUsers_positive() {

        doReturn(Mono.just(2L)).when(persistence).deleteUnregisteredUsers(any(LocalDateTime.class));

        userDetailsService.deleteUnregisteredUsers()
                .as(StepVerifier::create)
                .assertNext(count -> assertThat(count, is(2L)))
                .verifyComplete();

        verify(persistence, times(1)).deleteUnregisteredUsers(any(LocalDateTime.class));

        final ArgumentCaptor<LocalDateTime> argumentCaptorExists = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(persistence).deleteUnregisteredUsers(argumentCaptorExists.capture());

        assertThat(argumentCaptorExists.getValue().isAfter(LocalDateTime.now().minusMinutes(1)), is(true));
        assertThat(argumentCaptorExists.getValue().isBefore(LocalDateTime.now()), is(true));
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
