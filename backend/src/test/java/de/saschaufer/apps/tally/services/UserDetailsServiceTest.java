package de.saschaufer.apps.tally.services;

import de.saschaufer.apps.tally.config.security.JwtProperties;
import de.saschaufer.apps.tally.controller.dto.PostLoginResponse;
import de.saschaufer.apps.tally.management.UserAgent;
import de.saschaufer.apps.tally.persistence.Persistence;
import de.saschaufer.apps.tally.persistence.dto.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
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
    private UserDetailsService userDetailsService;

    @BeforeEach
    void beforeEach() {
        persistence = Mockito.mock(Persistence.class);
        jwtProperties = Mockito.mock(JwtProperties.class);
        jwtEncoder = Mockito.mock(JwtEncoder.class);
        userAgent = Mockito.mock(UserAgent.class);
        userDetailsService = new UserDetailsService(persistence, jwtProperties, jwtEncoder, userAgent);
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
}
