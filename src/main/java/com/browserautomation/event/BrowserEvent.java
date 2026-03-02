package com.browserautomation.event;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Base class for all browser automation events.
 * Events are typed messages dispatched through the EventBus to enable
 * decoupled communication between components (watchdogs, agent, tools).
 *
 */
public class BrowserEvent {

    private final String eventId;
    private final String eventType;
    private final Instant createdAt;
    private final String parentEventId;
    private final long timeoutMs;
    private final Map<String, Object> data;

    protected BrowserEvent(String eventType, Map<String, Object> data, String parentEventId, long timeoutMs) {
        this.eventId = UUID.randomUUID().toString();
        this.eventType = eventType;
        this.createdAt = Instant.now();
        this.parentEventId = parentEventId;
        this.timeoutMs = timeoutMs;
        this.data = data != null ? Map.copyOf(data) : Map.of();
    }

    protected BrowserEvent(String eventType, Map<String, Object> data) {
        this(eventType, data, null, 30000);
    }

    protected BrowserEvent(String eventType) {
        this(eventType, null, null, 30000);
    }

    public String getEventId() { return eventId; }
    public String getEventType() { return eventType; }
    public Instant getCreatedAt() { return createdAt; }
    public String getParentEventId() { return parentEventId; }
    public long getTimeoutMs() { return timeoutMs; }
    public Map<String, Object> getData() { return data; }

    /**
     * Get a typed data value.
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) data.get(key);
    }

    /**
     * Get a typed data value with default.
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, T defaultValue) {
        Object val = data.get(key);
        return val != null ? (T) val : defaultValue;
    }

    @Override
    public String toString() {
        return eventType + "{id=" + eventId.substring(0, 8) + ", data=" + data + "}";
    }
}
