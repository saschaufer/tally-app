package de.saschaufer.apps.tally.services;

import de.saschaufer.apps.tally.config.security.JwtProperties;
import de.saschaufer.apps.tally.management.UserAgent;
import de.saschaufer.apps.tally.persistence.Persistence;
import de.saschaufer.apps.tally.persistence.dto.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.ReactiveUserDetailsPasswordService;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jose.jws.JwsAlgorithms;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserDetailsService implements ReactiveUserDetailsService, ReactiveUserDetailsPasswordService {

    private final Persistence persistence;
    private final JwtProperties jwtProperties;
    private final JwtEncoder jwtEncoder;
    private final UserAgent userAgent;

    @Override
    public Mono<UserDetails> findByUsername(final String username) {
        return persistence.selectUser(username)
                .onErrorMap(err -> new BadCredentialsException("Error finding user", err))
                .doOnError(err -> log.atError().setMessage("Error finding user.").setCause(err).log())
                .doOnSuccess(user -> log.atInfo().setMessage("User found.").log())
                .map(user -> user);
    }

    @Override
    public Mono<UserDetails> updatePassword(final UserDetails user, final String newPassword) {

        log.atInfo().setMessage("Updating password for user '{}'.").addArgument(user.getUsername()).log();

        final Mono<User> selectUser = persistence.selectUser(user.getUsername());

        final Mono<Void> updateUser = selectUser.flatMap(u -> persistence.updateUserPassword(u.getId(), newPassword));

        return updateUser
                .doOnSuccess(v -> log.atInfo().setMessage("Password updated.").log())
                .doOnError(err -> log.atInfo().setMessage("Password not updated.").setCause(err).log())
                .then(Mono.just(user).map(u -> {
                    ((User) u).setPassword(newPassword);
                    return u;
                }));
    }

    public String createJwtToken(final User user) {

        final String username = user.getUsername();
        final List<String> authorities = user.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
        final String issuer = jwtProperties.issuer() == null ? userAgent.getHostName() : jwtProperties.issuer();
        final String audience = jwtProperties.audience() == null ? userAgent.getAppName() : jwtProperties.audience();
        final Instant issuedAt = Instant.now();

        final JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .audience(List.of(audience))
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plusSeconds(36000L)) // 10h
                .subject(username)
                .claim("authorities", authorities)
                .build();

        final JwsHeader header = JwsHeader.with(() -> JwsAlgorithms.HS256).build();

        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
