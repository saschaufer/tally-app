package de.saschaufer.apps.tally.config.security;

import de.saschaufer.apps.tally.controller.Handler;
import de.saschaufer.apps.tally.controller.Router;
import de.saschaufer.apps.tally.persistence.dto.User;
import de.saschaufer.apps.tally.services.UserDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jose.jws.JwsAlgorithms;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Base64;
import java.util.List;

import static de.saschaufer.apps.tally.persistence.dto.User.Role.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

@WebFluxTest(controllers = Router.class)
@Import({SecurityConfig.class, SecurityConfigSetup.TestJwtProperties.class, UserDetailsService.class, Handler.class})
public abstract class SecurityConfigSetup {

    static class TestJwtProperties {

        @Bean
        private JwtProperties jwtProperties() {
            return new JwtProperties("issuer", "audience", "key-aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", true);
        }
    }

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final String PASSWORD = "password";

    @Autowired
    private ApplicationContext context;

    @Autowired
    private JwtEncoder jwtEncoder;

    @MockBean
    protected UserDetailsService userDetailsService;

    protected WebTestClient webClient;

    @BeforeEach
    protected void beforeEach() {

        doAnswer(invocation -> {
            final String password = "{bcrypt}" + encoder.encode(PASSWORD);
            return switch (invocation.getArgument(0, String.class)) {
                case NONE -> Mono.just(new User(1L, NONE, password, NONE));
                case USER -> Mono.just(new User(2L, USER, password, USER));
                case ADMIN -> Mono.just(new User(3L, ADMIN, password, String.join(",", USER, ADMIN)));
                case INVITATION -> Mono.just(new User(4L, INVITATION, password, INVITATION));
                case null, default -> Mono.error(new BadCredentialsException("Error finding user"));
            };
        }).when(userDetailsService).findByUsername(any(String.class));

        webClient = WebTestClient
                .bindToApplicationContext(context)
                .configureClient()
                .build();
    }

    protected String credentials(final String user, final boolean passwordOk) {
        final byte[] bytes = (user + ":" + (passwordOk ? PASSWORD : PASSWORD + "-wrong")).getBytes();
        return Base64.getEncoder().encodeToString(bytes);
    }

    protected String testJwt(final String user) {
        return testJwt(user, Instant.now(), List.of(user));
    }

    protected String testJwt(final String user, final List<String> roles) {
        return testJwt(user, Instant.now(), roles);
    }

    protected String testJwt(final String user, final Instant issuedAt) {
        return testJwt(user, issuedAt, List.of(user));
    }

    protected String testJwt(final String user, final Instant issuedAt, final List<String> roles) {
        final JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plusSeconds(1L))
                .subject(user)
                .claim("authorities", roles)
                .build();

        final JwsHeader header = JwsHeader.with(() -> JwsAlgorithms.HS256).build();

        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
