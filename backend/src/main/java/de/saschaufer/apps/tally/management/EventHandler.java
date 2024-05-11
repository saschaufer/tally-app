package de.saschaufer.apps.tally.management;

import de.saschaufer.apps.tally.persistence.Persistence;
import de.saschaufer.apps.tally.persistence.dto.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventHandler {

    private final UserAgent userAgent;
    private final PasswordEncoder passwordEncoder;
    private final Persistence persistence;

    @EventListener(classes = ApplicationReadyEvent.class)
    void handleApplicationReadyEvent(final ApplicationReadyEvent event) {

        log.atInfo().setMessage("Ready. I am {}.").addArgument(userAgent.getFullName()).log();

        Mono.just(new User(null, "user", passwordEncoder.encode("password"), String.join(",", User.Role.USER, User.Role.ADMIN)))
                .flatMap(persistence::insertUser)
                .subscribe(
                        ok -> log.atInfo().setMessage("User created.").log(),
                        err -> log.atInfo().setMessage("Error creating user.").setCause(err).log()
                );
    }
}
