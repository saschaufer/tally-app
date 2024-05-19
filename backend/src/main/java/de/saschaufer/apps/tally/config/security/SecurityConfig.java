package de.saschaufer.apps.tally.config.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import de.saschaufer.apps.tally.persistence.dto.User;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtGrantedAuthoritiesConverterAdapter;
import org.springframework.security.oauth2.server.resource.web.access.server.BearerTokenServerAccessDeniedHandler;
import org.springframework.security.oauth2.server.resource.web.server.BearerTokenServerAuthenticationEntryPoint;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtProperties jwtProperties;

    /**
     * Provides a list of password encoders for matching incoming clear text passwords with encoded passwords in the
     * database. If, in a future version, the default password encoder changes, the stored passwords are updated to the
     * new default encoder once a user logs in. This happens in
     * {@link de.saschaufer.apps.tally.services.UserDetailsService#updatePassword(UserDetails, String)}.
     *
     * @return @{@link PasswordEncoder} to encode passwords with.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(final ServerHttpSecurity serverHttpSecurity) {

        return serverHttpSecurity
                .authorizeExchange(authorize -> authorize

                        // Static resources are available without authorization
                        .pathMatchers(HttpMethod.GET, "/", "*.js", "*.css", "*.ico", "media/*.woff2").permitAll()

                        // All backend endpoints need authorization
                        .pathMatchers(HttpMethod.POST, "/login").hasAnyAuthority(User.Role.USER)
                        .pathMatchers(HttpMethod.POST, "/register").hasAnyAuthority(User.Role.INVITATION)
                        .pathMatchers(HttpMethod.POST, "/settings/change-password").hasAnyAuthority(User.Role.USER)
                        .pathMatchers(HttpMethod.POST, "/settings/change-invitation-code").hasAnyAuthority(User.Role.ADMIN)
                        .anyExchange().authenticated()
                )
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(withDefaults())
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .logout(ServerHttpSecurity.LogoutSpec::disable)
                .oauth2ResourceServer(oAuth2 -> oAuth2.jwt(withDefaults()))
                .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(new BearerTokenServerAuthenticationEntryPoint())
                        .accessDeniedHandler(new BearerTokenServerAccessDeniedHandler())
                )
                .build();
    }

    @Bean
    protected NimbusReactiveJwtDecoder jwtDecoder() {

        final byte[] key = jwtProperties.key().getBytes(StandardCharsets.UTF_8);
        final String algorithm = Pbkdf2PasswordEncoder.SecretKeyFactoryAlgorithm.PBKDF2WithHmacSHA512.name();

        final SecretKey secretKey = new SecretKeySpec(key, algorithm);
        return NimbusReactiveJwtDecoder.withSecretKey(secretKey).build();
    }

    @Bean
    protected JwtEncoder jwtEncoder() {

        final byte[] key = jwtProperties.key().getBytes(StandardCharsets.UTF_8);
        final String algorithm = Pbkdf2PasswordEncoder.SecretKeyFactoryAlgorithm.PBKDF2WithHmacSHA512.name();

        final SecretKey secretKey = new SecretKeySpec(key, algorithm);
        final JWKSource<SecurityContext> jwkSource = new ImmutableSecret<>(secretKey);
        return new NimbusJwtEncoder(jwkSource);
    }

    @Bean
    public ReactiveJwtAuthenticationConverter jwtAuthenticationConverter() {

        // Don't read the roles from claim 'scope' but from 'authorities' of the JWT.
        // Don't prefix the roles with 'SCOPE_'.

        final JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        grantedAuthoritiesConverter.setAuthorityPrefix("");
        grantedAuthoritiesConverter.setAuthoritiesClaimName("authorities");

        final ReactiveJwtAuthenticationConverter jwtAuthenticationConverter = new ReactiveJwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(new ReactiveJwtGrantedAuthoritiesConverterAdapter(grantedAuthoritiesConverter));
        return jwtAuthenticationConverter;
    }
}
