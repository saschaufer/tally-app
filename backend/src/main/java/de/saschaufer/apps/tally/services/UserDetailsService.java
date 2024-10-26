package de.saschaufer.apps.tally.services;

import de.saschaufer.apps.tally.config.admin.AdminProperties;
import de.saschaufer.apps.tally.config.email.EmailProperties;
import de.saschaufer.apps.tally.config.security.JwtProperties;
import de.saschaufer.apps.tally.controller.dto.GetUsersResponse;
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
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserDetailsService implements ReactiveUserDetailsService, ReactiveUserDetailsPasswordService {

    private static final SecureRandom random = new SecureRandom();
    private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";


    private final Persistence persistence;
    private final JwtProperties jwtProperties;
    private final AdminProperties adminProperties;
    private final EmailProperties emailProperties;
    private final JwtEncoder jwtEncoder;
    private final UserAgent userAgent;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Mono<UserDetails> findByUsername(final String email) {
        return persistence.selectUser(email)
                .onErrorMap(err -> new BadCredentialsException("Error finding user", err))
                .doOnError(err -> log.atError().setMessage("Error finding user.").setCause(err).log())
                .doOnSuccess(user -> log.atInfo().setMessage("User found.").log())
                .map(user -> user);
    }

    public Mono<List<GetUsersResponse>> findAllUsers() {

        return persistence.selectUsers()
                .flatMap(users -> persistence.selectPaymentsSumAllUsers()
                        .map(payments -> Tuples.of(users, payments))
                )
                .flatMap(tuple -> persistence.selectPurchasesSumAllUsers()
                        .map(purchases -> Tuples.of(tuple.getT1(), tuple.getT2(), purchases))
                )
                .map(tuple -> {

                    final List<User> users = tuple.getT1();
                    final Map<Long, BigDecimal> payments = tuple.getT2();
                    final Map<Long, BigDecimal> purchases = tuple.getT3();

                    final List<GetUsersResponse> responses = new ArrayList<>();

                    for (final User user : users) {

                        final List<String> roles = new ArrayList<>();

                        if (user.getRoles() != null) {
                            roles.addAll(Arrays.asList(user.getRoles().split(",")));
                        }

                        if (adminProperties.emails().contains(user.getEmail())) {
                            roles.add(User.Role.ADMIN);
                        }

                        final BigDecimal paymentsTotal = payments.get(user.getId());
                        final BigDecimal purchasesTotal = purchases.get(user.getId());
                        final BigDecimal accountBalance = calcTotal(paymentsTotal, purchasesTotal);

                        final GetUsersResponse res = new GetUsersResponse(
                                user.getEmail(),
                                user.getRegistrationOn(),
                                user.getRegistrationComplete(),
                                roles,
                                accountBalance
                        );

                        responses.add(res);
                    }

                    return responses;
                });
    }

    private BigDecimal calcTotal(final BigDecimal paymentsTotal, final BigDecimal purchasesTotal) {

        final BigDecimal payments = paymentsTotal == null ? BigDecimal.ZERO : paymentsTotal;
        final BigDecimal purchases = purchasesTotal == null ? BigDecimal.ZERO : purchasesTotal;

        return payments.subtract(purchases);
    }

    public Mono<Void> deleteUser(final Long userId) {
        return persistence.deleteUser(userId);
    }

    public Mono<User> checkRegistered(final User user) {

        return Mono.just(user)
                .flatMap(u -> {
                    if (u.getRegistrationComplete().equals(Boolean.FALSE)) {
                        return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Registration is not completed"));
                    }
                    return Mono.just(u);
                });
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

    public Mono<Tuple2<String, String>> resetPassword(final String email) {

        log.atInfo().setMessage("Resetting password for user '{}'.").addArgument(email).log();

        return persistence.selectUser(email)
                .flatMap(this::checkRegistered)
                .map(u -> {
                    final StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < 6; i++) {
                        sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
                    }
                    return Tuples.of(u, sb.toString());
                })
                .flatMap(t -> changePassword(t.getT1(), t.getT2())
                        .then(Mono.just(Tuples.of(t.getT1().getEmail(), t.getT2())))
                );
    }

    public Mono<UserDetails> changePassword(final User user, final String newPassword) {

        final String encodedPassword = passwordEncoder.encode(newPassword);

        return updatePassword(user, encodedPassword);
    }

    public PostLoginResponse createJwtToken(final User user) {

        final List<String> authorities = new ArrayList<>(user.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList());

        if (adminProperties.emails().contains(user.getEmail())) {
            authorities.add(User.Role.ADMIN);
        }

        final String email = user.getEmail();
        final String issuer = jwtProperties.issuer() == null ? userAgent.getHostName() : jwtProperties.issuer();
        final String audience = jwtProperties.audience() == null ? userAgent.getAppName() : jwtProperties.audience();
        final Instant issuedAt = Instant.now();

        final JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .audience(List.of(audience))
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plusSeconds(36000L)) // 10h
                .subject(email)
                .claim("authorities", authorities)
                .build();

        final JwsHeader header = JwsHeader.with(() -> JwsAlgorithms.HS256).build();

        final String jwt = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

        return new PostLoginResponse(jwt, jwtProperties.secure());
    }

    public Mono<User> createUser(final String email, final String password, final List<String> roles) {

        final User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRoles(String.join(",", roles));
        user.setRegistrationSecret(String.valueOf(random.nextInt(97942 - 16234 + 1) + 16234)); // Number between 16234 and 97942.
        user.setRegistrationOn(LocalDateTime.now());
        user.setRegistrationComplete(false);

        return persistence.existsUser(email)
                .flatMap(found -> {
                    if (found.equals(Boolean.TRUE)) {
                        return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is taken"));
                    }

                    return Mono.just(user)
                            .flatMap(persistence::insertUser);
                });
    }

    public Mono<User> checkRegistrationSecret(final String email, final String givenRegistrationSecret) {

        return findByUsername(email)
                .map(User.class::cast)
                .flatMap(user -> {
                    if (!user.getRegistrationSecret().equals(givenRegistrationSecret)) {
                        return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "The registration secret is not correct"));
                    }
                    return Mono.just(user);
                });
    }

    public Mono<Void> updateUserRegistrationComplete(final String email) {

        log.atInfo().setMessage("Confirming user registration for user '{}'.").addArgument(email).log();

        return persistence.updateUserRegistrationComplete(email)
                .doOnSuccess(v -> log.atInfo().setMessage("User registration confirmed.").log())
                .doOnError(err -> log.atInfo().setMessage("Error confirming user registration.").setCause(err).log());
    }

    public Mono<Long> deleteUnregisteredUsers() {
        final LocalDateTime deleteRegisteredBefore = LocalDateTime.now().minus(emailProperties.deleteUnregisteredUsersAfter());
        return persistence.deleteUnregisteredUsers(deleteRegisteredBefore);
    }

    public void createInvitationCodeIfNoneExists() {

        persistence.existsUser("invitation-code")
                .flatMap(found -> {
                    if (found.equals(Boolean.TRUE)) {
                        return Mono.empty();
                    }

                    final String password = UUID.randomUUID().toString();

                    final User user = new User();
                    user.setEmail("invitation-code");
                    user.setPassword(passwordEncoder.encode(password));
                    user.setRoles(User.Role.INVITATION);
                    user.setRegistrationSecret("00000");
                    user.setRegistrationOn(LocalDateTime.now());
                    user.setRegistrationComplete(true);

                    return Mono.just(user)
                            .flatMap(persistence::insertUser)
                            .map(u -> password);
                })
                .subscribe(
                        password -> log.atInfo().setMessage("Invitation code created: {}").addArgument(password).log(),
                        error -> log.atInfo().setMessage("Error creating invitation code if not exists.").setCause(error).log()
                );
    }
}
