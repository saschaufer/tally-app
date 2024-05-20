package de.saschaufer.apps.tally.controller;

import de.saschaufer.apps.tally.controller.dto.*;
import de.saschaufer.apps.tally.persistence.dto.User;
import de.saschaufer.apps.tally.services.ProductService;
import de.saschaufer.apps.tally.services.UserDetailsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.UnsupportedMediaTypeStatusException;
import reactor.core.Exceptions;
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
    private final ProductService productService;

    public Mono<ServerResponse> postLogin(final ServerRequest request) {

        log.atInfo().setMessage("Login.").log();

        final Mono<User> user = request.principal().map(Authentication.class::cast)
                .map(a -> (User) a.getPrincipal())
                .doOnNext(u -> log.atInfo().setMessage("User '{}' logged in.").addArgument(u.getUsername()).log());

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

    public Mono<ServerResponse> postChangePassword(final ServerRequest request) {

        log.atInfo().setMessage("Change password.").log();

        final Mono<User> user = request.principal().map(Authentication.class::cast)
                .map(auth -> switch (auth.getPrincipal()) {
                    case UserDetails u -> u.getUsername();
                    case Jwt j -> j.getSubject();
                    default ->
                            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown authentication method");
                })
                .flatMap(userDetailsService::findByUsername)
                .map(u -> (User) u);

        final Mono<String> newPassword = request.bodyToMono(String.class)
                .switchIfEmpty(badRequest("Body required"));

        return user.zipWith(newPassword, Pair::of)
                .flatMap(pair -> userDetailsService.changePassword(pair.getFirst(), pair.getSecond()))
                .flatMap(u -> ok().build())
                .doOnError(e -> log.atError().setMessage("Error changing password.").setCause(e).log())
                .onErrorResume(this::buildErrorResponse);
    }

    public Mono<ServerResponse> postChangeInvitationCode(final ServerRequest request) {

        log.atInfo().setMessage("Change invitation code.").log();

        final Mono<User> invitationCodeUser = userDetailsService.findByUsername("invitation-code")
                .map(u -> (User) u);

        final Mono<String> newInvitationCode = request.bodyToMono(String.class)
                .switchIfEmpty(badRequest("Body required"));

        return invitationCodeUser.zipWith(newInvitationCode, Pair::of)
                .flatMap(pair -> userDetailsService.changePassword(pair.getFirst(), pair.getSecond()))
                .flatMap(u -> ok().build())
                .doOnError(e -> log.atError().setMessage("Error changing invitation code.").setCause(e).log())
                .onErrorResume(this::buildErrorResponse);
    }

    public Mono<ServerResponse> postCreateProduct(final ServerRequest request) {

        log.atInfo().setMessage("Create product.").log();

        return request.bodyToMono(PostCreateProductRequest.class)
                .switchIfEmpty(badRequest("Body required"))
                .flatMap(RequestBodyValidator::validate)

                .flatMap(r -> productService.createProduct(r.name(), r.price()))

                .then(Mono.defer(() -> ok().build()))
                .doOnError(e -> log.atError().setMessage("Error creating new product.").setCause(e).log())
                .onErrorResume(this::buildErrorResponse);
    }

    public Mono<ServerResponse> getReadProducts(final ServerRequest request) {

        log.atInfo().setMessage("Read products.").log();

        return productService.readProducts()
                .flatMap(response -> ok().contentType(MediaType.APPLICATION_JSON).bodyValue(response))
                .doOnError(e -> log.atError().setMessage("Error reading products.").setCause(e).log())
                .onErrorResume(this::buildErrorResponse);
    }

    public Mono<ServerResponse> postUpdateProduct(final ServerRequest request) {

        log.atInfo().setMessage("Update product.").log();

        return request.bodyToMono(PostUpdateProductRequest.class)
                .switchIfEmpty(badRequest("Body required"))
                .flatMap(RequestBodyValidator::validate)

                .flatMap(r -> productService.updateProduct(r.id(), r.name()))

                .then(Mono.defer(() -> ok().build()))
                .doOnError(e -> log.atError().setMessage("Error updating product.").setCause(e).log())
                .onErrorResume(this::buildErrorResponse);
    }

    public Mono<ServerResponse> postUpdateProductPrice(final ServerRequest request) {

        log.atInfo().setMessage("Update product price.").log();

        return request.bodyToMono(PostUpdateProductPriceRequest.class)
                .switchIfEmpty(badRequest("Body required"))
                .flatMap(RequestBodyValidator::validate)

                .flatMap(r -> productService.updateProductPrice(r.id(), r.price()))

                .then(Mono.defer(() -> ok().build()))
                .doOnError(e -> log.atError().setMessage("Error updating product price.").setCause(e).log())
                .onErrorResume(this::buildErrorResponse);
    }

    private <T> Mono<T> badRequest(final String message) {
        return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, message));
    }

    private Mono<ServerResponse> buildErrorResponse(final Throwable t) {

        return switch (Exceptions.unwrap(t)) {
            case UnsupportedMediaTypeStatusException e -> {
                final String supported = String.join(", ", e.getSupportedMediaTypes().stream()
                        .map(mediaType -> String.format("'%s'", mediaType)).toList()
                );

                yield status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                        .contentType(MediaType.TEXT_PLAIN)
                        .bodyValue(String.format("Content type '%s' not supported. Supported: %s", e.getContentType(), supported));
            }
            case ResponseStatusException e -> status(e.getStatusCode())
                    .contentType(MediaType.TEXT_PLAIN)
                    .bodyValue(Objects.requireNonNull(e.getReason()));
            default -> status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        };
    }
}
