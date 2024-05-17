package de.saschaufer.apps.tally.services;

import de.saschaufer.apps.tally.config.security.JwtProperties;
import de.saschaufer.apps.tally.controller.dto.PostLoginResponse;
import de.saschaufer.apps.tally.management.UserAgent;
import de.saschaufer.apps.tally.persistence.Persistence;
import de.saschaufer.apps.tally.persistence.dto.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.ReactiveUserDetailsPasswordService;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.JwsAlgorithms;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserDetailsService implements ReactiveUserDetailsService, ReactiveUserDetailsPasswordService {

    private final Persistence persistence;
    private final JwtProperties jwtProperties;
    private final JwtEncoder jwtEncoder;
    private final UserAgent userAgent;
    private final PasswordEncoder passwordEncoder;

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

    public PostLoginResponse createJwtToken(final User user) {

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

        final String jwt = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

        return new PostLoginResponse(jwt, jwtProperties.secure());
    }

    public Mono<User> createUser(final String username, final String password, final List<String> roles) {

        final User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRoles(String.join(",", roles));

        return persistence.existsUser(username)
                .flatMap(found -> {
                    if (found.equals(Boolean.TRUE)) {
                        return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username is taken"));
                    }

                    return Mono.just(user)
                            .flatMap(persistence::insertUser);
                });
    }

    public void createAdminIfNotExists() {

        createUserIfNotExists("admin", String.join(",", User.Role.USER, User.Role.ADMIN))
                .subscribe(
                        password -> log.atInfo().setMessage("Created admin user 'admin' with password: {}").addArgument(password).log(),
                        error -> log.atInfo().setMessage("Error creating admin user if not exists.").setCause(error).log()
                );
    }

    public void createInvitationCodeIfNoneExists() {

        createUserIfNotExists("invitation-code", User.Role.INVITATION)
                .subscribe(
                        password -> log.atInfo().setMessage("Invitation code created: {}").addArgument(password).log(),
                        error -> log.atInfo().setMessage("Error creating invitation code if not exists.").setCause(error).log()
                );
    }

    private Mono<String> createUserIfNotExists(final String username, final String roles) {
        return persistence.existsUser(username)
                .flatMap(found -> {
                    if (found.equals(Boolean.TRUE)) {
                        return Mono.empty();
                    }

                    final String password = UUID.randomUUID().toString();

                    final User user = new User();
                    user.setUsername(username);
                    user.setPassword(passwordEncoder.encode(password));
                    user.setRoles(roles);

                    return Mono.just(user)
                            .flatMap(persistence::insertUser)
                            .map(u -> password);
                });
    }
}
