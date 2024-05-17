package de.saschaufer.apps.tally.controller;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.Set;

@NoArgsConstructor(access = AccessLevel.NONE)
public class RequestBodyValidator {

    private static final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    private static final jakarta.validation.Validator validator = factory.getValidator();

    public static <T> Mono<T> validate(final T object) {

        final Set<ConstraintViolation<Object>> violations = validator.validate(object);

        if (violations.isEmpty()) {
            return Mono.just(object);
        }

        return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, violations.iterator().next().getMessage()));
    }
}
