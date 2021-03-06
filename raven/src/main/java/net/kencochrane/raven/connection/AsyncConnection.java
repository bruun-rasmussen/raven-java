package net.kencochrane.raven.connection;

import net.kencochrane.raven.event.Event;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Asynchronous usage of a connection.
 * <p>
 * Instead of synchronously sending each event to a connection, use a ThreadPool to establish the connection
 * and submit the event.
 * </p>
 */
public class AsyncConnection implements Connection {
    private static final Logger logger = Logger.getLogger(AsyncConnection.class.getCanonicalName());
    /**
     * Timeout of the {@link #executorService}.
     */
    private static final int SHUTDOWN_TIMEOUT = 1000;
    /**
     * Connection used to actually send the events.
     */
    private final Connection actualConnection;
    /**
     * Option to disable the propagation of the {@link #close()} operation to the actual connection.
     */
    private final boolean propagateClose;
    /**
     * Executor service in charge of running the connection in separate threads.
     */
    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    /**
     * Creates a connection which will rely on an executor to send events.
     * <p>
     * Will propagate the {@link #close()} operation.
     * </p>
     *
     * @param actualConnection connection used to send the events.
     */
    public AsyncConnection(Connection actualConnection) {
        this(actualConnection, true);
    }

    /**
     * Creates a connection which will rely on an executor to send events.
     *
     * @param actualConnection connection used to send the events.
     * @param propagateClose   whether or not the {@link #actualConnection} should be closed
     *                         when this connection closes.
     */
    public AsyncConnection(Connection actualConnection, boolean propagateClose) {
        this.actualConnection = actualConnection;
        this.propagateClose = propagateClose;
        addShutdownHook();
    }

    /**
     * Adds a hook to shutdown the {@link #executorService} gracefully when the JVM shuts down.
     */
    private void addShutdownHook() {
        // JUL loggers are shutdown by an other shutdown hook, it's possible that nothing will get actually logged.
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                try {
                    AsyncConnection.this.close();
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "An exception occurred while closing the connection.", e);
                }
            }
        });
    }

    /**
     * {@inheritDoc}
     * <p>
     * The event will be added to a queue and will be handled by a separate {@code Thread} later on.
     * </p>
     */
    @Override
    public void send(Event event) {
        executorService.execute(new EventSubmitter(event));
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Closing the {@link AsyncConnection} will attempt a graceful shutdown of the {@link #executorService} with a
     * timeout of {@link #SHUTDOWN_TIMEOUT}, allowing the current events to be submitted while new events will
     * be rejected.<br />
     * If the shutdown times out, the {@code executorService} will be forced to shutdown.
     * </p>
     */
    @Override
    public void close() throws IOException {
        logger.log(Level.INFO, "Gracefully shutdown sentry threads.");
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(SHUTDOWN_TIMEOUT, TimeUnit.MILLISECONDS)) {
                logger.log(Level.WARNING, "Graceful shutdown took too much time, forcing the shutdown.");
                List<Runnable> tasks = executorService.shutdownNow();
                logger.log(Level.INFO, tasks.size() + " tasks failed to execute before the shutdown.");
            }
            logger.log(Level.SEVERE, "Shutdown finished.");
        } catch (InterruptedException e) {
            logger.log(Level.SEVERE, "Graceful shutdown interrupted, forcing the shutdown.");
            List<Runnable> tasks = executorService.shutdownNow();
            logger.log(Level.INFO, tasks.size() + " tasks failed to execute before the shutdown.");
        } finally {
            if (propagateClose)
                actualConnection.close();
        }
    }

    public void setExecutorService(ExecutorService executorService) {
        this.executorService = executorService;
    }

    /**
     * Simple runnable using the {@link #send(net.kencochrane.raven.event.Event)} method of the
     * {@link #actualConnection}.
     */
    private final class EventSubmitter implements Runnable {
        private final Event event;

        private EventSubmitter(Event event) {
            this.event = event;
        }

        @Override
        public void run() {
            try {
                actualConnection.send(event);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "An exception occurred while sending the event to Sentry.", e);
            }
        }
    }
}
