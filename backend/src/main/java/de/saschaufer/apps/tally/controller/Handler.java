package de.saschaufer.apps.tally.controller;

import de.saschaufer.apps.tally.controller.dto.*;
import de.saschaufer.apps.tally.persistence.dto.User;
import de.saschaufer.apps.tally.services.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.UnsupportedMediaTypeStatusException;
import reactor.core.Exceptions;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Objects;

import static de.saschaufer.apps.tally.controller.MDCFilter.KEY_MDC;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;
import static org.springframework.web.reactive.function.server.ServerResponse.status;

@Slf4j
@Component
@RequiredArgsConstructor
public class Handler {

    private final UserDetailsService userDetailsService;
    private final ProductService productService;
    private final PurchaseService purchaseService;
    private final EmailService emailService;
    private final PaymentService paymentService;

    public Mono<ServerResponse> postLogin(final ServerRequest request) {

        return setMdc(request)
                .doOnNext(r -> log.atInfo().setMessage("Login.").log())

                // Get user
                .flatMap(ServerRequest::principal)
                .map(Authentication.class::cast)
                .map(a -> (User) a.getPrincipal())
                .flatMap(userDetailsService::checkRegistered)
                .doOnNext(u -> log.atInfo().setMessage("User '{}' logged in.").addArgument(u.getEmail()).log())
                .map(userDetailsService::createJwtToken)

                // Build response
                .flatMap(res -> ok().contentType(MediaType.APPLICATION_JSON).bodyValue(res))

                // Build error response
                .doOnError(e -> log.atError().setMessage("Error logging in.").setCause(e).log())
                .onErrorResume(this::buildErrorResponse);
    }

    public Mono<ServerResponse> postRegisterNewUser(final ServerRequest request) {

        return setMdc(request)
                .doOnNext(r -> log.atInfo().setMessage("Register new user.").log())

                // Register user
                .flatMap(r -> r.bodyToMono(PostRegisterNewUserRequest.class))
                .switchIfEmpty(badRequest("Body required"))
                .flatMap(RequestBodyValidator::validate)
                .flatMap(user -> userDetailsService.createUser(user.email(), user.password(), List.of(User.Role.USER)))
                .flatMap(user -> Mono.fromCallable(() -> {
                    emailService.sendRegistrationEmail(user.getEmail(), user.getRegistrationSecret());
                    return user;
                }).subscribeOn(Schedulers.boundedElastic()))

                // Build response
                .flatMap(user -> ok().build())

                // Build error response
                .doOnError(e -> log.atError().setMessage("Error creating new user.").setCause(e).log())
                .onErrorResume(this::buildErrorResponse);
    }

    public Mono<ServerResponse> postRegisterNewUserConfirm(final ServerRequest request) {

        return setMdc(request)
                .doOnNext(r -> log.atInfo().setMessage("Confirm register new user.").log())

                // Confirm registration
                .flatMap(r -> r.bodyToMono(PostRegisterNewUserConfirmRequest.class))
                .switchIfEmpty(badRequest("Body required"))
                .flatMap(RequestBodyValidator::validate)
                .flatMap(confirmRequest -> userDetailsService.checkRegistrationSecret(confirmRequest.email(), confirmRequest.registrationSecret()))
                .flatMap(user -> userDetailsService.updateUserRegistrationComplete(user.getEmail()))

                // Build response
                .then(Mono.defer(() -> ok().build()))

                // Build error response
                .doOnError(e -> log.atError().setMessage("Error confirming registration of new user.").setCause(e).log())
                .onErrorResume(this::buildErrorResponse);
    }

    public Mono<ServerResponse> postResetPassword(final ServerRequest request) {

        return setMdc(request)
                .doOnNext(r -> log.atInfo().setMessage("Reset password.").log())

                // Reset password
                .flatMap(r -> r.bodyToMono(String.class))
                .switchIfEmpty(badRequest("Body required"))
                .flatMap(userDetailsService::resetPassword)
                .flatMap(t -> Mono.fromCallable(() -> {
                    final String email = t.getT1();
                    final String password = t.getT2();
                    emailService.sendResetPasswordEmail(email, password);
                    return t;
                }).subscribeOn(Schedulers.boundedElastic()))

                // Build response
                .then(Mono.defer(() -> ok().build()))

                // Build error response
                .doOnError(e -> log.atError().setMessage("Error resetting password.").setCause(e).log())
                .onErrorResume(this::buildErrorResponse);
    }

    public Mono<ServerResponse> postChangePassword(final ServerRequest request) {

        // Get user
        final Mono<User> user = setMdc(request)
                .doOnNext(r -> log.atInfo().setMessage("Change password.").log())
                .flatMap(ServerRequest::principal)
                .map(Authentication.class::cast)
                .map(auth -> switch (auth.getPrincipal()) {
                    case UserDetails u -> u.getUsername();
                    case Jwt j -> j.getSubject();
                    default ->
                            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown authentication method");
                })
                .flatMap(userDetailsService::findByUsername)
                .map(u -> (User) u);

        // Get body
        final Mono<String> newPassword = request.bodyToMono(String.class)
                .switchIfEmpty(badRequest("Body required"));

        // Change password
        return user.zipWith(newPassword, Pair::of)
                .flatMap(pair -> userDetailsService.changePassword(pair.getFirst(), pair.getSecond()))

                // Build response
                .flatMap(u -> ok().build())

                // Build error response
                .doOnError(e -> log.atError().setMessage("Error changing password.").setCause(e).log())
                .onErrorResume(this::buildErrorResponse);
    }

    public Mono<ServerResponse> postChangeInvitationCode(final ServerRequest request) {

        // Get invitation user
        final Mono<User> invitationCodeUser = setMdc(request)
                .doOnNext(r -> log.atInfo().setMessage("Change invitation code.").log())
                .flatMap(r -> userDetailsService.findByUsername("invitation-code"))
                .map(u -> (User) u);

        // Get body
        final Mono<String> newInvitationCode = request.bodyToMono(String.class)
                .switchIfEmpty(badRequest("Body required"));

        // Change invitation code (password of invitation user)
        return invitationCodeUser.zipWith(newInvitationCode, Pair::of)
                .flatMap(pair -> userDetailsService.changePassword(pair.getFirst(), pair.getSecond()))

                // Build response
                .flatMap(u -> ok().build())

                // Build error response
                .doOnError(e -> log.atError().setMessage("Error changing invitation code.").setCause(e).log())
                .onErrorResume(this::buildErrorResponse);
    }

    public Mono<ServerResponse> postCreateProduct(final ServerRequest request) {

        return setMdc(request)
                .doOnNext(r -> log.atInfo().setMessage("Create product.").log())

                // Create product
                .flatMap(r -> r.bodyToMono(PostCreateProductRequest.class))
                .switchIfEmpty(badRequest("Body required"))
                .flatMap(RequestBodyValidator::validate)
                .flatMap(r -> productService.createProduct(r.name(), r.price()))

                // Build response
                .then(Mono.defer(() -> ok().build()))

                // Build error response
                .doOnError(e -> log.atError().setMessage("Error creating new product.").setCause(e).log())
                .onErrorResume(this::buildErrorResponse);
    }

    public Mono<ServerResponse> postReadProduct(final ServerRequest request) {

        return setMdc(request)
                .doOnNext(r -> log.atInfo().setMessage("Read product.").log())

                // Get Product
                .flatMap(r -> r.bodyToMono(PostReadProductRequest.class))
                .switchIfEmpty(badRequest("Body required"))
                .flatMap(RequestBodyValidator::validate)
                .flatMap(r -> productService.readProduct(r.id()))

                // Build response
                .flatMap(response -> ok().contentType(MediaType.APPLICATION_JSON).bodyValue(response))

                // Build error response
                .doOnError(e -> log.atError().setMessage("Error reading product.").setCause(e).log())
                .onErrorResume(this::buildErrorResponse);
    }

    public Mono<ServerResponse> getReadProducts(final ServerRequest request) {

        return setMdc(request)
                .doOnNext(r -> log.atInfo().setMessage("Read products.").log())

                // Get products
                .flatMap(r -> productService.readProducts())

                // Build response
                .flatMap(response -> ok().contentType(MediaType.APPLICATION_JSON).bodyValue(response))

                // Build error response
                .doOnError(e -> log.atError().setMessage("Error reading products.").setCause(e).log())
                .onErrorResume(this::buildErrorResponse);
    }

    public Mono<ServerResponse> postUpdateProduct(final ServerRequest request) {

        return setMdc(request)
                .doOnNext(r -> log.atInfo().setMessage("Update product.").log())

                // Update product
                .flatMap(r -> r.bodyToMono(PostUpdateProductRequest.class))
                .switchIfEmpty(badRequest("Body required"))
                .flatMap(RequestBodyValidator::validate)
                .flatMap(r -> productService.updateProduct(r.id(), r.name()))

                // Build response
                .then(Mono.defer(() -> ok().build()))

                // Build error response
                .doOnError(e -> log.atError().setMessage("Error updating product.").setCause(e).log())
                .onErrorResume(this::buildErrorResponse);
    }

    public Mono<ServerResponse> postUpdateProductPrice(final ServerRequest request) {

        return setMdc(request)
                .doOnNext(r -> log.atInfo().setMessage("Update product price.").log())

                // Update product price
                .flatMap(r -> r.bodyToMono(PostUpdateProductPriceRequest.class))
                .switchIfEmpty(badRequest("Body required"))
                .flatMap(RequestBodyValidator::validate)
                .flatMap(r -> productService.updateProductPrice(r.id(), r.price()))

                // Build response
                .then(Mono.defer(() -> ok().build()))

                // Build error response
                .doOnError(e -> log.atError().setMessage("Error updating product price.").setCause(e).log())
                .onErrorResume(this::buildErrorResponse);
    }

    public Mono<ServerResponse> postCreatePurchase(final ServerRequest request) {

        // Get user
        final Mono<User> user = setMdc(request)
                .doOnNext(r -> log.atInfo().setMessage("Create purchase.").log())

                .flatMap(ServerRequest::principal)
                .map(Authentication.class::cast)
                .map(auth -> switch (auth.getPrincipal()) {
                    case UserDetails u -> u.getUsername();
                    case Jwt j -> j.getSubject();
                    default ->
                            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown authentication method");
                })
                .flatMap(userDetailsService::findByUsername)
                .map(u -> (User) u);

        // Get body
        final Mono<PostCreatePurchaseRequest> purchase = request.bodyToMono(PostCreatePurchaseRequest.class)
                .switchIfEmpty(badRequest("Body required"))
                .flatMap(RequestBodyValidator::validate);

        // Create purchase
        return user.zipWith(purchase, Pair::of)
                .flatMap(pair -> purchaseService.createPurchase(pair.getFirst().getId(), pair.getSecond().productId()))

                // Build response
                .then(Mono.defer(() -> ok().build()))

                // Build error response
                .doOnError(e -> log.atError().setMessage("Error creating new purchase.").setCause(e).log())
                .onErrorResume(this::buildErrorResponse);
    }

    public Mono<ServerResponse> getReadPurchases(final ServerRequest request) {

        // Get user
        final Mono<User> user = setMdc(request)
                .doOnNext(r -> log.atInfo().setMessage("Read purchases.").log())

                .flatMap(ServerRequest::principal)
                .map(Authentication.class::cast)
                .map(auth -> switch (auth.getPrincipal()) {
                    case UserDetails u -> u.getUsername();
                    case Jwt j -> j.getSubject();
                    default ->
                            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown authentication method");
                })
                .flatMap(userDetailsService::findByUsername)
                .map(u -> (User) u);

        // Read purchases
        return user.flatMap(u -> purchaseService.readPurchases(u.getId()))

                // Build response
                .flatMap(response -> ok().contentType(MediaType.APPLICATION_JSON).bodyValue(response))

                // Build error response
                .doOnError(e -> log.atError().setMessage("Error reading purchases.").setCause(e).log())
                .onErrorResume(this::buildErrorResponse);
    }

    public Mono<ServerResponse> postDeletePurchase(final ServerRequest request) {

        return setMdc(request)
                .doOnNext(r -> log.atInfo().setMessage("Delete purchase.").log())

                // Delete prurchase
                .flatMap(r -> r.bodyToMono(PostDeletePurchaseRequest.class))
                .switchIfEmpty(badRequest("Body required"))
                .flatMap(RequestBodyValidator::validate)
                .flatMap(p -> purchaseService.deletePurchase(p.purchaseId()))

                // Build response
                .then(Mono.defer(() -> ok().build()))

                // Build error response
                .doOnError(e -> log.atError().setMessage("Error deleting purchase.").setCause(e).log())
                .onErrorResume(this::buildErrorResponse);
    }

    public Mono<ServerResponse> postCreatePayment(final ServerRequest request) {

        // Get user
        final Mono<User> user = setMdc(request)
                .doOnNext(r -> log.atInfo().setMessage("Create payment.").log())
                .flatMap(ServerRequest::principal)
                .map(Authentication.class::cast)
                .map(auth -> switch (auth.getPrincipal()) {
                    case UserDetails u -> u.getUsername();
                    case Jwt j -> j.getSubject();
                    default ->
                            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown authentication method");
                })
                .flatMap(userDetailsService::findByUsername)
                .map(u -> (User) u);

        // Get body
        final Mono<PostCreatePaymentRequest> payment = request.bodyToMono(PostCreatePaymentRequest.class)
                .switchIfEmpty(badRequest("Body required"))
                .flatMap(RequestBodyValidator::validate);

        // Create payment
        return user.zipWith(payment).flatMap(pair -> paymentService.createPayment(pair.getT1().getId(), pair.getT2().amount()))

                // Build response
                .then(Mono.defer(() -> ok().build()))

                // Build error response
                .doOnError(e -> log.atError().setMessage("Error creating payment.").setCause(e).log())
                .onErrorResume(this::buildErrorResponse);
    }

    public Mono<ServerResponse> getReadPayments(final ServerRequest request) {

        // Get user
        final Mono<User> user = setMdc(request)
                .doOnNext(r -> log.atInfo().setMessage("Read payments.").log())

                .flatMap(ServerRequest::principal)
                .map(Authentication.class::cast)
                .map(auth -> switch (auth.getPrincipal()) {
                    case UserDetails u -> u.getUsername();
                    case Jwt j -> j.getSubject();
                    default ->
                            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown authentication method");
                })
                .flatMap(userDetailsService::findByUsername)
                .map(u -> (User) u);

        // Read payments
        return user.flatMap(u -> paymentService.readPayments(u.getId()))

                // Build response
                .flatMap(response -> ok().contentType(MediaType.APPLICATION_JSON).bodyValue(response))

                // Build error response
                .doOnError(e -> log.atError().setMessage("Error reading payments.").setCause(e).log())
                .onErrorResume(this::buildErrorResponse);
    }

    public Mono<ServerResponse> postDeletePayment(final ServerRequest request) {

        return setMdc(request)
                .doOnNext(r -> log.atInfo().setMessage("Delete payment.").log())

                // Delete payment
                .flatMap(r -> r.bodyToMono(PostDeletePaymentRequest.class))
                .switchIfEmpty(badRequest("Body required"))
                .flatMap(RequestBodyValidator::validate)
                .flatMap(p -> paymentService.deletePayment(p.paymentId()))

                // Build response
                .then(Mono.defer(() -> ok().build()))

                // Build error response
                .doOnError(e -> log.atError().setMessage("Error deleting payment.").setCause(e).log())
                .onErrorResume(this::buildErrorResponse);
    }

    public Mono<ServerResponse> getReadAccountBalance(final ServerRequest request) {

        // Get user
        final Mono<User> user = setMdc(request)
                .doOnNext(r -> log.atInfo().setMessage("Read account balance.").log())

                .flatMap(ServerRequest::principal)
                .map(Authentication.class::cast)
                .map(auth -> switch (auth.getPrincipal()) {
                    case UserDetails u -> u.getUsername();
                    case Jwt j -> j.getSubject();
                    default ->
                            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown authentication method");
                })
                .flatMap(userDetailsService::findByUsername)
                .map(u -> (User) u);

        // Read account balance
        return user.flatMap(u -> paymentService.readAccountBalance(u.getId()))

                // Build response
                .flatMap(response -> ok().contentType(MediaType.APPLICATION_JSON).bodyValue(response))

                // Build error response
                .doOnError(e -> log.atError().setMessage("Error reading account balance.").setCause(e).log())
                .onErrorResume(this::buildErrorResponse);
    }

    private Mono<ServerRequest> setMdc(final ServerRequest request) {
        return Mono.deferContextual(ctx -> {
            MDC.setContextMap(ctx.get(KEY_MDC));
            return Mono.just(request);
        });
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
