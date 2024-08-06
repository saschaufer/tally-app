package de.saschaufer.apps.tally.management;

import de.saschaufer.apps.tally.services.UserDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class EventHandlerTest {

    private UserAgent userAgent;
    private UserDetailsService userDetailsService;
    private EventHandler eventHandler;

    @BeforeEach
    void beforeEach() {
        userAgent = mock(UserAgent.class);
        userDetailsService = mock(UserDetailsService.class);
        eventHandler = new EventHandler(userAgent, userDetailsService);
    }

    @Test
    void handleApplicationReadyEvent_positive() {

        doReturn("tally").when(userAgent).getFullName();

        eventHandler.handleApplicationReadyEvent(null);

        verify(userAgent, times(1)).getFullName();
        verify(userDetailsService, times(1)).createInvitationCodeIfNoneExists();
    }
}
