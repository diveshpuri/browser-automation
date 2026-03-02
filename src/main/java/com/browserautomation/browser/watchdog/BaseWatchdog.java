package com.browserautomation.browser.watchdog;

import com.browserautomation.event.BrowserEvent;
import com.browserautomation.event.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Base class for all specialized watchdogs.
 * Each watchdog subscribes to specific events on the EventBus and
 * performs monitoring/handling duties.
 */
public abstract class BaseWatchdog implements AutoCloseable {

    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected final EventBus eventBus;
    protected final ScheduledExecutorService scheduler;
    private ScheduledFuture<?> periodicTask;
    private volatile boolean running;

    protected BaseWatchdog(EventBus eventBus) {
        this.eventBus = eventBus;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, getWatchdogName() + "-watchdog");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * Get the name of this watchdog for logging and identification.
     */
    public abstract String getWatchdogName();

    /**
     * Subscribe to relevant events. Called during start().
     */
    protected abstract void subscribeToEvents();

    /**
     * Start the watchdog.
     */
    public void start() {
        if (running) return;
        running = true;
        subscribeToEvents();
        logger.info("{} watchdog started", getWatchdogName());
    }

    /**
     * Stop the watchdog.
     */
    public void stop() {
        running = false;
        if (periodicTask != null) {
            periodicTask.cancel(false);
        }
        logger.info("{} watchdog stopped", getWatchdogName());
    }

    /**
     * Schedule a periodic check.
     */
    protected void schedulePeriodicCheck(Runnable check, long intervalMs) {
        periodicTask = scheduler.scheduleAtFixedRate(() -> {
            if (running) {
                try {
                    check.run();
                } catch (Exception e) {
                    logger.warn("{} periodic check error: {}", getWatchdogName(), e.getMessage());
                }
            }
        }, intervalMs, intervalMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Dispatch an event through the bus.
     */
    protected void dispatchEvent(BrowserEvent event) {
        eventBus.dispatch(event);
    }

    public boolean isRunning() { return running; }

    @Override
    public void close() {
        stop();
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
