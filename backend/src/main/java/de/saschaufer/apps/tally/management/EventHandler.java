package de.saschaufer.apps.tally.management;

import de.saschaufer.apps.tally.services.UserDetailsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventHandler {

    private final UserAgent userAgent;
    private final UserDetailsService userDetailsService;

    @EventListener(classes = ApplicationReadyEvent.class)
    void handleApplicationReadyEvent(final ApplicationReadyEvent event) {

        log.atInfo().setMessage("Ready. I am {}.").addArgument(userAgent.getFullName()).log();

        userDetailsService.createInvitationCodeIfNoneExists();

        Flux.interval(Duration.ofMillis(0), Duration.ofMinutes(5))
                .onBackpressureDrop()
                .flatMap(ignore -> userDetailsService.deleteUnregisteredUsers(), 1)
                .subscribe(
                        count -> log.atInfo().setMessage("Deleted unregistered users: {}.").addArgument(count).log(),
                        error -> log.atInfo().setMessage("Error deleting unregistered users.").setCause(error).log()
                );
    }
}
