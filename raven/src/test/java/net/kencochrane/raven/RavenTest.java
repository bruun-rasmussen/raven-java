package net.kencochrane.raven;

import net.kencochrane.raven.connection.Connection;
import net.kencochrane.raven.event.EventBuilder;
import net.kencochrane.raven.event.LoggedEvent;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class RavenTest {
    @Mock
    private Connection mockConnection;
    private Raven raven;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        raven = new Raven(mockConnection);
    }

    @Test
    public void testSendEvent() {
        LoggedEvent loggedEvent = mock(LoggedEvent.class);
        raven.sendEvent(loggedEvent);

        verify(mockConnection).send(loggedEvent);
    }

    @Test
    public void testSendEventBuilder() {
        EventBuilder eventBuilder = mock(EventBuilder.class);
        raven.sendEvent(eventBuilder);

        verify(eventBuilder).build();
        verify(mockConnection).send(any(LoggedEvent.class));
    }
}