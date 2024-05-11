package de.saschaufer.apps.tally.controller;

import de.saschaufer.apps.tally.persistence.dto.User;
import de.saschaufer.apps.tally.services.UserDetailsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import static org.springframework.web.reactive.function.server.ServerResponse.ok;

@Slf4j
@Component
@RequiredArgsConstructor
public class Handler {

    private final UserDetailsService userDetailsService;

    public Mono<ServerResponse> postLogin(final ServerRequest request) {

        log.atInfo().setMessage("Login.").log();

        final Mono<User> user = request.principal().map(Authentication.class::cast)
                .map(a -> (User) a.getPrincipal())
                .doOnNext(auth -> log.atInfo().setMessage("User '{}' logged in.").addArgument(auth.getUsername()).log());

        final Mono<String> jwt = user.map(userDetailsService::createJwtToken);

        return ok().contentType(MediaType.TEXT_PLAIN)
                .body(BodyInserters.fromPublisher(jwt, String.class));
    }

    public Mono<ServerResponse> getNone(final ServerRequest request) {
        log.atInfo().setMessage("none").log();
        return ok().contentType(MediaType.TEXT_PLAIN).body(Mono.just("none"), String.class);
    }

    public Mono<ServerResponse> getUser(final ServerRequest request) {
        log.atInfo().setMessage("user").log();
        return ok().contentType(MediaType.TEXT_PLAIN).body(Mono.just("user"), String.class);
    }

    public Mono<ServerResponse> getAdmin(final ServerRequest request) {
        log.atInfo().setMessage("admin").log();
        return ok().contentType(MediaType.TEXT_PLAIN).body(Mono.just("admin"), String.class);
    }

    public Mono<ServerResponse> getUseradmin(final ServerRequest request) {
        log.atInfo().setMessage("useradmin").log();
        return ok().contentType(MediaType.TEXT_PLAIN).body(Mono.just("useradmin"), String.class);
    }
}
