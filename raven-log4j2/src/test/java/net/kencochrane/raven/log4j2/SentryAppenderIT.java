package net.kencochrane.raven.log4j2;

import net.kencochrane.raven.SentryStub;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class SentryAppenderIT {
    private static final Logger logger = LogManager.getLogger(SentryAppenderIT.class);
    private SentryStub sentryStub;

    @Before
    public void setUp() {
        sentryStub = new SentryStub();
    }

    @After
    public void tearDown() {
        sentryStub.removeEvents();
    }

    @Test
    public void testInfoLog() {
        assertThat(sentryStub.getEventCount(), is(0));
        logger.info("This is a test");
        assertThat(sentryStub.getEventCount(), is(1));
    }
}
