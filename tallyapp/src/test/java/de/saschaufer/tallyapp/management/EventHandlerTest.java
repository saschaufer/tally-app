package de.saschaufer.tallyapp.management;

import de.saschaufer.tallyapp.services.UserDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

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

        doReturn(Mono.just(0L)).when(userDetailsService).deleteUnregisteredUsers();
    }

    @Test
    void handleApplicationReadyEvent_positive() {

        doReturn("tally").when(userAgent).getFullName();

        eventHandler.handleApplicationReadyEvent(null);

        verify(userAgent, times(1)).getFullName();
        verify(userDetailsService, times(1)).createInvitationCodeIfNoneExists();
        verify(userDetailsService, timeout(1000).times(1)).deleteUnregisteredUsers();
    }
}
