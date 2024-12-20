package de.saschaufer.tallyapp.config.security;

import de.saschaufer.tallyapp.controller.Handler;
import de.saschaufer.tallyapp.controller.Router;
import de.saschaufer.tallyapp.persistence.dto.User;
import de.saschaufer.tallyapp.services.*;
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

import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

import static de.saschaufer.tallyapp.persistence.dto.User.Role.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

@WebFluxTest(controllers = Router.class)
@Import({SecurityConfig.class, SecurityConfigSetup.TestJwtProperties.class, UserDetailsService.class, Handler.class})
public abstract class SecurityConfigSetup {

    static class TestJwtProperties {

        @Bean
        private JwtProperties jwtProperties() {
            return new JwtProperties("issuer", "audience", Duration.ofHours(1L), "key-aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", true);
        }
    }

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    protected final String PASSWORD = "password";
    protected final String ENCODED_PASSWORD = "{bcrypt}" + encoder.encode(PASSWORD);

    @Autowired
    private ApplicationContext context;

    @Autowired
    private JwtEncoder jwtEncoder;

    @MockBean
    protected UserDetailsService userDetailsService;

    @MockBean
    protected ProductService productService;

    @MockBean
    protected PurchaseService purchaseService;

    @MockBean
    protected EmailService emailService;

    @MockBean
    protected PaymentService paymentService;

    protected WebTestClient webClient;

    @BeforeEach
    protected void beforeEach() {

        doAnswer(invocation ->
                getUserByUsername(invocation.getArgument(0, String.class))
        ).when(userDetailsService).findByUsername(any(String.class));

        webClient = WebTestClient
                .bindToApplicationContext(context)
                .configureClient()
                .build();
    }

    protected Mono<User> getUserByUsername(final String name) {
        final String password = ENCODED_PASSWORD;
        //@formatter:off
        return switch (name) {
            case NONE -> Mono.just(new User(1L, NONE, password, NONE, null, Instant.parse("2024-01-01T01:01:01Z"), true));
            case USER -> Mono.just(new User(2L, USER, password, USER, "registration-secret-user", Instant.parse("2024-01-01T01:01:01Z"), true));
            case ADMIN -> Mono.just(new User(3L, ADMIN, password, String.join(",", USER, ADMIN), "registration-secret-admin", Instant.parse("2024-01-01T01:01:01Z"), true));
            case INVITATION -> Mono.just(new User(4L, INVITATION, password, INVITATION, null, Instant.parse("2024-01-01T01:01:01Z"), true));
            case "invitation-code" -> Mono.just(new User(5L, "invitation-code", password, INVITATION, null, Instant.parse("2024-01-01T01:01:01Z"), true));
            case null, default -> Mono.error(new BadCredentialsException("Error finding user"));
        };
        //@formatter:on
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
