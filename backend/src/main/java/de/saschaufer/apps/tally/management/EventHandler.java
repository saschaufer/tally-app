package de.saschaufer.apps.tally.management;

import de.saschaufer.apps.tally.services.UserDetailsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

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
    }
}
