package de.saschaufer.apps.tally.services;

import de.saschaufer.apps.tally.config.admin.AdminProperties;
import de.saschaufer.apps.tally.config.email.EmailProperties;
import de.saschaufer.apps.tally.config.security.JwtProperties;
import de.saschaufer.apps.tally.controller.dto.GetUsersResponse;
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
import org.springframework.security.oauth2.jose.jws.JwsAlgorithms;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
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

        final UserDetails userDetails = new User(1L, "username@mail.com", "password", "roles", "registration-secret", Instant.parse("2024-05-19T23:54:01Z"), true);

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
    void findAllUsers_positive() {

        doReturn(List.of("2@mail", "5@mail")).when(adminProperties).emails();

        doReturn(Mono.just(List.of(
                new User(1L, "1@mail", null, "role1,role2", null, Instant.parse("2024-01-02T03:04:01Z"), true),
                new User(2L, "2@mail", null, "role3", null, Instant.parse("2024-01-02T03:04:02Z"), false),
                new User(3L, "3@mail", null, "role2,role1,role3", null, Instant.parse("2024-01-02T03:04:03Z"), true),
                new User(4L, "4@mail", null, null, null, Instant.parse("2024-01-02T03:04:04Z"), true),
                new User(5L, "5@mail", null, ",", null, Instant.parse("2024-01-02T03:04:05Z"), true)
        ))).when(persistence).selectUsers();
        doReturn(Mono.just(Map.of(
                2L, BigDecimal.TEN,
                1L, BigDecimal.TWO,
                4L, BigDecimal.ONE
        ))).when(persistence).selectPaymentsSumAllUsers();
        doReturn(Mono.just(Map.of(
                1L, BigDecimal.ZERO,
                2L, BigDecimal.ONE,
                5L, BigDecimal.ONE
        ))).when(persistence).selectPurchasesSumAllUsers();

        userDetailsService.findAllUsers()
                .as(StepVerifier::create)
                .assertNext(getUsersResponses -> {
                    assertThat(getUsersResponses.size(), is(5));

                    assertThat(getUsersResponses.getFirst(), is(new GetUsersResponse("1@mail", Instant.parse("2024-01-02T03:04:01Z"), true, List.of("role1", "role2"), BigDecimal.TWO)));
                    assertThat(getUsersResponses.get(1), is(new GetUsersResponse("2@mail", Instant.parse("2024-01-02T03:04:02Z"), false, List.of("role3", "admin"), new BigDecimal("9"))));
                    assertThat(getUsersResponses.get(2), is(new GetUsersResponse("3@mail", Instant.parse("2024-01-02T03:04:03Z"), true, List.of("role2", "role1", "role3"), BigDecimal.ZERO)));
                    assertThat(getUsersResponses.get(3), is(new GetUsersResponse("4@mail", Instant.parse("2024-01-02T03:04:04Z"), true, List.of(), BigDecimal.ONE)));
                    assertThat(getUsersResponses.getLast(), is(new GetUsersResponse("5@mail", Instant.parse("2024-01-02T03:04:05Z"), true, List.of("admin"), new BigDecimal("-1"))));
                })
                .verifyComplete();

        verify(persistence, times(1)).selectUsers();
        verify(persistence, times(1)).selectPaymentsSumAllUsers();
        verify(persistence, times(1)).selectPurchasesSumAllUsers();
    }

    @Test
    void findAllUsers_positive_NoUsers() {

        doReturn(Mono.just(List.of())).when(persistence).selectUsers();
        doReturn(Mono.just(Map.of(2L, BigDecimal.TEN))).when(persistence).selectPaymentsSumAllUsers();
        doReturn(Mono.just(Map.of(1L, BigDecimal.ZERO))).when(persistence).selectPurchasesSumAllUsers();

        userDetailsService.findAllUsers()
                .as(StepVerifier::create)
                .assertNext(getUsersResponses -> assertThat(getUsersResponses.size(), is(0)))
                .verifyComplete();

        verify(persistence, times(1)).selectUsers();
        verify(persistence, times(1)).selectPaymentsSumAllUsers();
        verify(persistence, times(1)).selectPurchasesSumAllUsers();
    }

    @Test
    void findAllUsers_negative_SelectUsersThrowsException() {

        doReturn(Mono.error(new RuntimeException("Bad"))).when(persistence).selectUsers();

        userDetailsService.findAllUsers()
                .as(StepVerifier::create)
                .verifyErrorSatisfies(error -> {
                    assertThat(error.getMessage(), containsString("Bad"));
                });

        verify(persistence, times(1)).selectUsers();
        verify(persistence, times(0)).selectPaymentsSumAllUsers();
        verify(persistence, times(0)).selectPurchasesSumAllUsers();
    }

    @Test
    void findAllUsers_negative_SelectPaymentsSumAllUsersThrowsException() {

        doReturn(Mono.just(List.of())).when(persistence).selectUsers();
        doReturn(Mono.error(new RuntimeException("Bad"))).when(persistence).selectPaymentsSumAllUsers();

        userDetailsService.findAllUsers()
                .as(StepVerifier::create)
                .verifyErrorSatisfies(error -> {
                    assertThat(error.getMessage(), containsString("Bad"));
                });

        verify(persistence, times(1)).selectUsers();
        verify(persistence, times(1)).selectPaymentsSumAllUsers();
        verify(persistence, times(0)).selectPurchasesSumAllUsers();
    }

    @Test
    void findAllUsers_negative_SelectPurchasesSumAllUsersThrowsException() {

        doReturn(Mono.just(List.of())).when(persistence).selectUsers();
        doReturn(Mono.just(Map.of())).when(persistence).selectPaymentsSumAllUsers();
        doReturn(Mono.error(new RuntimeException("Bad"))).when(persistence).selectPurchasesSumAllUsers();

        userDetailsService.findAllUsers()
                .as(StepVerifier::create)
                .verifyErrorSatisfies(error -> {
                    assertThat(error.getMessage(), containsString("Bad"));
                });

        verify(persistence, times(1)).selectUsers();
        verify(persistence, times(1)).selectPaymentsSumAllUsers();
        verify(persistence, times(1)).selectPurchasesSumAllUsers();
    }

    @Test
    void deleteUser_positive() {

        doReturn(Mono.empty()).when(persistence).deleteUser(any(Long.class));

        Mono.just(1L).flatMap(userDetailsService::deleteUser)
                .as(StepVerifier::create)
                .expectNext()
                .verifyComplete();

        verify(persistence, times(1)).deleteUser(1L);
    }

    @Test
    void deleteUser_negative_DeleteUserFails() {

        doReturn(Mono.error(new RuntimeException("Bad"))).when(persistence).deleteUser(any(Long.class));

        Mono.just(1L).flatMap(userDetailsService::deleteUser)
                .as(StepVerifier::create)
                .verifyErrorSatisfies(error -> {
                    assertThat(error.getMessage(), containsString("Bad"));
                });

        verify(persistence, times(1)).deleteUser(1L);
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

        final UserDetails userDetails = new User(1L, "username@mail.com", "password", "roles", "registration-secret", Instant.parse("2024-05-19T23:54:01Z"), true);

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

        final UserDetails userDetails = new User(1L, "username@mail.com", "password", "roles", "registration-secret", Instant.parse("2024-05-19T23:54:01Z"), true);

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

        final UserDetails userDetails = new User(1L, "username@mail.com", "password", "roles", "registration-secret", Instant.parse("2024-05-19T23:54:01Z"), true);

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
    void resetPassword_positive() {

        final UserDetails userDetails = new User(1L, "username@mail.com", "password", "roles", "registration-secret", Instant.parse("2024-05-19T23:54:01Z"), true);

        doReturn(Mono.just(userDetails)).when(persistence).selectUser(any(String.class));
        doReturn(Mono.empty()).when(persistence).updateUserPassword(any(Long.class), any(String.class));
        doReturn("pwd").when(passwordEncoder).encode(any(String.class));

        Mono.just("username@mail.com")
                .flatMap(userDetailsService::resetPassword)
                .as(StepVerifier::create)
                .assertNext(t -> {
                    assertThat(t.getT1(), is("username@mail.com"));
                    assertThat(t.getT2(), notNullValue());
                    assertThat(t.getT2(), not(is("pwd")));
                })
                .verifyComplete();

        verify(persistence, times(2)).selectUser("username@mail.com");
        verify(persistence, times(1)).updateUserPassword(1L, "pwd");
    }

    @Test
    void resetPassword_negative_UserNotExists() {

        doReturn(Mono.error(new RuntimeException("User not found"))).when(persistence).selectUser(any(String.class));

        Mono.just("username@mail.com")
                .flatMap(userDetailsService::resetPassword)
                .as(StepVerifier::create)
                .verifyErrorSatisfies(error ->
                        assertThat(error.getMessage(), containsString("User not found"))
                );

        verify(persistence, times(1)).selectUser(any(String.class));
        verify(persistence, times(0)).updateUserPassword(any(Long.class), any(String.class));
    }

    @Test
    void resetPassword_negative_UserNotRegistered() {

        final UserDetails userDetails = new User(1L, "username@mail.com", "password", "roles", "registration-secret", Instant.parse("2024-05-19T23:54:01Z"), false);

        doReturn(Mono.just(userDetails)).when(persistence).selectUser(any(String.class));

        Mono.just("username@mail.com")
                .flatMap(userDetailsService::resetPassword)
                .as(StepVerifier::create)
                .verifyErrorSatisfies(error -> {
                    assertThat(error, instanceOf(ResponseStatusException.class));
                    assertThat(error.getMessage(), containsString("Registration is not completed"));

                    final ResponseStatusException e = (ResponseStatusException) error;
                    assertThat(e.getStatusCode(), is(HttpStatus.UNAUTHORIZED));
                });

        verify(persistence, times(1)).selectUser(any(String.class));
        verify(persistence, times(0)).updateUserPassword(any(Long.class), any(String.class));
    }

    @Test
    void resetPassword_negative_UserNotUpdated() {

        final UserDetails userDetails = new User(1L, "username@mail.com", "password", "roles", "registration-secret", Instant.parse("2024-05-19T23:54:01Z"), true);

        doReturn(Mono.just(userDetails)).when(persistence).selectUser(any(String.class));
        doReturn(Mono.error(new RuntimeException("User not updated"))).when(persistence).updateUserPassword(any(Long.class), any(String.class));
        doReturn("pwd").when(passwordEncoder).encode(any(String.class));

        Mono.just("username@mail.com")
                .flatMap(userDetailsService::resetPassword)
                .as(StepVerifier::create)
                .verifyErrorSatisfies(error ->
                        assertThat(error.getMessage(), containsString("User not updated"))
                );

        verify(persistence, times(2)).selectUser(any(String.class));
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
    void createJwtToken_positive_AdminIssuerAudience() throws URISyntaxException, MalformedURLException {

        final User user = new User(1L, "username@mail.com", "password", User.Role.USER, null, null, null);

        doReturn(List.of("username@mail.com")).when(adminProperties).emails();

        doReturn("https://issuer.com").when(jwtProperties).issuer();
        doReturn("audience").when(jwtProperties).audience();
        doReturn(Duration.ofHours(1L)).when(jwtProperties).expirationTime();

        doReturn(new Jwt("ecoded-jwt", Instant.MIN, Instant.MAX, Map.of("header", "h"), Map.of("claim", "m"))).when(jwtEncoder).encode(any());

        final PostLoginResponse response = userDetailsService.createJwtToken(user);

        final ArgumentCaptor<JwtEncoderParameters> captor = ArgumentCaptor.forClass(JwtEncoderParameters.class);
        verify(jwtEncoder, times(1)).encode(captor.capture());
        verify(adminProperties, times(1)).emails();

        assertThat(response.jwt(), is("ecoded-jwt"));
        assertThat(response.secure(), is(jwtProperties.secure()));

        final JwtEncoderParameters parameters = captor.getValue();
        assertThat(parameters.getJwsHeader().getAlgorithm().getName(), is(JwsAlgorithms.HS256));
        assertThat(parameters.getClaims().getIssuer(), is(new URI("https://issuer.com").toURL()));
        assertThat(parameters.getClaims().getAudience(), is(List.of("audience")));
        assertThat(parameters.getClaims().getIssuedAt(), is(greaterThan(Instant.now().minusSeconds(1))));
        assertThat(parameters.getClaims().getIssuedAt(), is(lessThan(Instant.now().plusSeconds(10))));
        assertThat(parameters.getClaims().getExpiresAt(), is(greaterThan(Instant.now().plus(Duration.ofHours(1).minusSeconds(1)))));
        assertThat(parameters.getClaims().getExpiresAt(), is(lessThan(Instant.now().plus(Duration.ofHours(1).plusSeconds(10)))));
        assertThat(parameters.getClaims().getSubject(), is("username@mail.com"));
        assertThat(parameters.getClaims().getClaims().get("authorities"), is(List.of(User.Role.USER, User.Role.ADMIN)));
    }

    @Test
    void createJwtToken_positive_NoAdminIssuerAudience() throws URISyntaxException, MalformedURLException {

        final User user = new User(1L, "username@mail.com", "password", User.Role.USER, null, null, null);

        doReturn("https://host.com").when(userAgent).getHostName();
        doReturn("app").when(userAgent).getAppName();

        doReturn(Duration.ofHours(1L)).when(jwtProperties).expirationTime();

        doReturn(new Jwt("ecoded-jwt", Instant.MIN, Instant.MAX, Map.of("header", "h"), Map.of("claim", "m"))).when(jwtEncoder).encode(any());

        final PostLoginResponse response = userDetailsService.createJwtToken(user);

        final ArgumentCaptor<JwtEncoderParameters> captor = ArgumentCaptor.forClass(JwtEncoderParameters.class);
        verify(jwtEncoder, times(1)).encode(captor.capture());
        verify(adminProperties, times(1)).emails();

        assertThat(response.jwt(), is("ecoded-jwt"));
        assertThat(response.secure(), is(jwtProperties.secure()));

        final JwtEncoderParameters parameters = captor.getValue();
        assertThat(parameters.getJwsHeader().getAlgorithm().getName(), is(JwsAlgorithms.HS256));
        assertThat(parameters.getClaims().getIssuer(), is(new URI("https://host.com").toURL()));
        assertThat(parameters.getClaims().getAudience(), is(List.of("app")));
        assertThat(parameters.getClaims().getIssuedAt(), is(greaterThan(Instant.now().minusSeconds(1))));
        assertThat(parameters.getClaims().getIssuedAt(), is(lessThan(Instant.now().plusSeconds(10))));
        assertThat(parameters.getClaims().getExpiresAt(), is(greaterThan(Instant.now().plus(Duration.ofHours(1).minusSeconds(1)))));
        assertThat(parameters.getClaims().getExpiresAt(), is(lessThan(Instant.now().plus(Duration.ofHours(1).plusSeconds(10)))));
        assertThat(parameters.getClaims().getSubject(), is("username@mail.com"));
        assertThat(parameters.getClaims().getClaims().get("authorities"), is(List.of(User.Role.USER)));
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
        assertThat(user.getRegistrationOn().isAfter(Instant.now().minusSeconds(60)), is(true));
        assertThat(user.getRegistrationOn().isBefore(Instant.now()), is(true));
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

        doReturn(Mono.just(2L)).when(persistence).deleteUnregisteredUsers(any(Instant.class));

        userDetailsService.deleteUnregisteredUsers()
                .as(StepVerifier::create)
                .assertNext(count -> assertThat(count, is(2L)))
                .verifyComplete();

        verify(persistence, times(1)).deleteUnregisteredUsers(any(Instant.class));

        final ArgumentCaptor<Instant> argumentCaptorExists = ArgumentCaptor.forClass(Instant.class);
        verify(persistence).deleteUnregisteredUsers(argumentCaptorExists.capture());

        assertThat(argumentCaptorExists.getValue().isAfter(Instant.now().minusSeconds(60)), is(true));
        assertThat(argumentCaptorExists.getValue().isBefore(Instant.now()), is(true));
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
