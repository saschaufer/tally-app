package de.saschaufer.apps.tally.controller;

import de.saschaufer.apps.tally.controller.dto.PostRegisterNewUserRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

class RequestBodyValidatorTest {

    @Test
    void validatePostRegisterNewUser_positive() {

        Mono.just(new PostRegisterNewUserRequest("user", "password"))
                .flatMap(RequestBodyValidator::validate)
                .as(StepVerifier::create)
                .assertNext(request -> {
                    assertThat(request.username(), is("user"));
                    assertThat(request.password(), is("password"));
                })
                .verifyComplete();
    }

    @ParameterizedTest
    @MethodSource
    void validatePostRegisterNewUser_negative(final PostRegisterNewUserRequest request, final HttpStatus expectedStatus, final String expectedErrorMessage) {

        Mono.just(request)
                .flatMap(RequestBodyValidator::validate)
                .as(StepVerifier::create)
                .verifyErrorSatisfies(error -> {
                    assertThat(error, instanceOf(ResponseStatusException.class));

                    final ResponseStatusException e = (ResponseStatusException) error;
                    assertThat(e.getStatusCode(), is(expectedStatus));
                    assertThat(e.getReason(), is(expectedErrorMessage));
                });
    }

    static Stream<Arguments> validatePostRegisterNewUser_negative() {
        return Stream.of(
                Arguments.of(
                        new PostRegisterNewUserRequest(null, "password"),
                        HttpStatus.BAD_REQUEST,
                        "Username is required"
                ),
                Arguments.of(
                        new PostRegisterNewUserRequest("\t\r\n  ", "password"),
                        HttpStatus.BAD_REQUEST,
                        "Username is required"
                ),
                Arguments.of(
                        new PostRegisterNewUserRequest("user", null),
                        HttpStatus.BAD_REQUEST,
                        "Password is required"
                ),
                Arguments.of(
                        new PostRegisterNewUserRequest("user", "\t\r\n  "),
                        HttpStatus.BAD_REQUEST,
                        "Password is required"
                )
        );
    }
}
