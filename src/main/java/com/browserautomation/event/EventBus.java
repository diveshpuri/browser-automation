package com.browserautomation.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * Central event bus for dispatching typed events between components.
 * Supports synchronous and asynchronous event dispatch, event history,
 * and typed event subscriptions.
 *
 * Equivalent to browser-use's bubus EventBus.
 */
public class EventBus {

    private static final Logger logger = LoggerFactory.getLogger(EventBus.class);

    private final Map<String, List<EventHandler<?>>> handlers = new ConcurrentHashMap<>();
    private final Map<String, BrowserEvent> eventHistory = new ConcurrentHashMap<>();
    private final int maxHistorySize;
    private final ExecutorService asyncExecutor;

    public EventBus() {
        this(1000);
    }

    public EventBus(int maxHistorySize) {
        this.maxHistorySize = maxHistorySize;
        this.asyncExecutor = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "event-bus-async");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * Subscribe to events of a specific type.
     */
    public <T extends BrowserEvent> void subscribe(Class<T> eventClass, Consumer<T> handler) {
        String eventType = eventClass.getSimpleName();
        handlers.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>())
                .add(new EventHandler<>(eventClass, handler));
        logger.debug("Subscribed handler for event type: {}", eventType);
    }

    /**
     * Subscribe to events by type name string.
     */
    public void subscribe(String eventType, Consumer<BrowserEvent> handler) {
        handlers.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>())
                .add(new EventHandler<>(BrowserEvent.class, handler));
    }

    /**
     * Unsubscribe a handler from events of a specific type.
     */
    public <T extends BrowserEvent> void unsubscribe(Class<T> eventClass, Consumer<T> handler) {
        String eventType = eventClass.getSimpleName();
        List<EventHandler<?>> handlerList = handlers.get(eventType);
        if (handlerList != null) {
            handlerList.removeIf(h -> h.handler == handler);
        }
    }

    /**
     * Dispatch an event synchronously to all subscribers.
     */
    @SuppressWarnings("unchecked")
    public void dispatch(BrowserEvent event) {
        String eventType = event.getEventType();
        logger.debug("Dispatching event: {}", event);

        // Record in history
        recordEvent(event);

        // Notify handlers registered for this specific event type
        List<EventHandler<?>> handlerList = handlers.get(eventType);
        if (handlerList != null) {
            for (EventHandler<?> handler : handlerList) {
                try {
                    ((EventHandler<BrowserEvent>) handler).handler.accept(event);
                } catch (Exception e) {
                    logger.warn("Error in event handler for {}: {}", eventType, e.getMessage());
                }
            }
        }

        // Notify wildcard handlers (subscribed to "*")
        List<EventHandler<?>> wildcardHandlers = handlers.get("*");
        if (wildcardHandlers != null) {
            for (EventHandler<?> handler : wildcardHandlers) {
                try {
                    ((EventHandler<BrowserEvent>) handler).handler.accept(event);
                } catch (Exception e) {
                    logger.warn("Error in wildcard event handler: {}", e.getMessage());
                }
            }
        }
    }

    /**
     * Dispatch an event asynchronously.
     */
    public CompletableFuture<Void> dispatchAsync(BrowserEvent event) {
        return CompletableFuture.runAsync(() -> dispatch(event), asyncExecutor);
    }

    /**
     * Get event history sorted by creation time (most recent first).
     */
    public List<BrowserEvent> getRecentEvents(int limit) {
        return eventHistory.values().stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .limit(limit)
                .toList();
    }

    /**
     * Get event history for a specific event type.
     */
    public List<BrowserEvent> getEventsByType(String eventType) {
        return eventHistory.values().stream()
                .filter(e -> e.getEventType().equals(eventType))
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .toList();
    }

    /**
     * Get all events in history.
     */
    public Map<String, BrowserEvent> getEventHistory() {
        return Collections.unmodifiableMap(eventHistory);
    }

    /**
     * Clear event history.
     */
    public void clearHistory() {
        eventHistory.clear();
    }

    /**
     * Check if there are any subscribers for an event type.
     */
    public boolean hasSubscribers(String eventType) {
        List<EventHandler<?>> handlerList = handlers.get(eventType);
        return handlerList != null && !handlerList.isEmpty();
    }

    /**
     * Get the number of subscribers for an event type.
     */
    public int getSubscriberCount(String eventType) {
        List<EventHandler<?>> handlerList = handlers.get(eventType);
        return handlerList != null ? handlerList.size() : 0;
    }

    /**
     * Shutdown the async executor.
     */
    public void shutdown() {
        asyncExecutor.shutdown();
        try {
            if (!asyncExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                asyncExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            asyncExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private void recordEvent(BrowserEvent event) {
        if (eventHistory.size() >= maxHistorySize) {
            // Remove oldest event
            eventHistory.values().stream()
                    .min(Comparator.comparing(BrowserEvent::getCreatedAt))
                    .ifPresent(oldest -> eventHistory.remove(oldest.getEventId()));
        }
        eventHistory.put(event.getEventId(), event);
    }

    /**
     * Internal handler wrapper that pairs event type with consumer.
     */
    private static class EventHandler<T extends BrowserEvent> {
        final Class<T> eventClass;
        final Consumer<T> handler;

        EventHandler(Class<T> eventClass, Consumer<T> handler) {
            this.eventClass = eventClass;
            this.handler = handler;
        }
    }
}
