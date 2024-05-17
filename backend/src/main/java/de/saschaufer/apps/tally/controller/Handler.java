package de.saschaufer.apps.tally.controller;

import de.saschaufer.apps.tally.controller.dto.PostLoginResponse;
import de.saschaufer.apps.tally.controller.dto.PostRegisterNewUserRequest;
import de.saschaufer.apps.tally.persistence.dto.User;
import de.saschaufer.apps.tally.services.UserDetailsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.UnsupportedMediaTypeStatusException;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;

import static org.springframework.web.reactive.function.server.ServerResponse.ok;
import static org.springframework.web.reactive.function.server.ServerResponse.status;

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

        final Mono<PostLoginResponse> response = user.map(userDetailsService::createJwtToken);

        return ok().contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromPublisher(response, PostLoginResponse.class));
    }

    public Mono<ServerResponse> postRegisterNewUser(final ServerRequest request) {

        log.atInfo().setMessage("Register new user.").log();

        final Mono<User> createdUser = request.bodyToMono(PostRegisterNewUserRequest.class)
                .switchIfEmpty(badRequest("Body required"))
                .flatMap(RequestBodyValidator::validate)
                .flatMap(user -> userDetailsService.createUser(user.username(), user.password(), List.of(User.Role.USER)));

        return createdUser
                .flatMap(user -> ok().build())
                .doOnError(e -> log.atError().setMessage("Error creating new user.").setCause(e).log())
                .onErrorResume(this::buildErrorResponse);
    }

    private <T> Mono<T> badRequest(final String message) {
        return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, message));
    }

    private Mono<ServerResponse> buildErrorResponse(final Throwable t) {

        if (t instanceof UnsupportedMediaTypeStatusException e) {

            final String supported = String.join(", ", e.getSupportedMediaTypes().stream()
                    .map(mediaType -> String.format("'%s'", mediaType)).toList()
            );

            return status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).contentType(MediaType.TEXT_PLAIN)
                    .bodyValue(String.format("Content type '%s' not supported. Supported: %s", e.getContentType(), supported));
        }

        if (t instanceof ResponseStatusException e) {
            return status(e.getStatusCode()).contentType(MediaType.TEXT_PLAIN)
                    .bodyValue(Objects.requireNonNull(e.getReason()));
        }

        return status(HttpStatus.INTERNAL_SERVER_ERROR).build();
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
