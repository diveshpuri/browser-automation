package com.browserautomation.sync;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Data synchronization service between components.
 * Equivalent to browser-use's sync module.
 *
 * Provides a publish-subscribe mechanism for sharing state and data
 * between agents, browser sessions, and other components.
 */
public class DataSyncService implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(DataSyncService.class);

    private final Map<String, Object> dataStore;
    private final Map<String, List<Consumer<SyncEvent>>> subscribers;
    private final List<SyncEvent> eventLog;
    private final int maxEventLogSize;

    public DataSyncService() {
        this(1000);
    }

    public DataSyncService(int maxEventLogSize) {
        this.dataStore = new ConcurrentHashMap<>();
        this.subscribers = new ConcurrentHashMap<>();
        this.eventLog = new CopyOnWriteArrayList<>();
        this.maxEventLogSize = maxEventLogSize;
    }

    /**
     * Put a value into the shared data store and notify subscribers.
     *
     * @param key   the data key
     * @param value the value to store
     */
    public void put(String key, Object value) {
        Object previous = dataStore.put(key, value);
        SyncEvent event = new SyncEvent(
                previous == null ? SyncEvent.EventType.CREATED : SyncEvent.EventType.UPDATED,
                key, value, previous, System.currentTimeMillis()
        );
        recordEvent(event);
        notifySubscribers(key, event);
        logger.debug("Data sync: {} = {} ({})", key, value, event.getType());
    }

    /**
     * Get a value from the data store.
     */
    public Object get(String key) {
        return dataStore.get(key);
    }

    /**
     * Get a typed value from the data store.
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        Object value = dataStore.get(key);
        if (value == null) return null;
        if (type.isInstance(value)) {
            return (T) value;
        }
        throw new ClassCastException("Value for key '" + key + "' is " + value.getClass().getName() +
                ", expected " + type.getName());
    }

    /**
     * Get a value with a default if not present.
     */
    public Object getOrDefault(String key, Object defaultValue) {
        return dataStore.getOrDefault(key, defaultValue);
    }

    /**
     * Remove a value from the data store and notify subscribers.
     */
    public Object remove(String key) {
        Object removed = dataStore.remove(key);
        if (removed != null) {
            SyncEvent event = new SyncEvent(
                    SyncEvent.EventType.DELETED, key, null, removed, System.currentTimeMillis());
            recordEvent(event);
            notifySubscribers(key, event);
        }
        return removed;
    }

    /**
     * Check if a key exists.
     */
    public boolean containsKey(String key) {
        return dataStore.containsKey(key);
    }

    /**
     * Get all keys.
     */
    public java.util.Set<String> keys() {
        return dataStore.keySet();
    }

    /**
     * Get all data as a map.
     */
    public Map<String, Object> getAll() {
        return new LinkedHashMap<>(dataStore);
    }

    /**
     * Subscribe to changes for a specific key.
     *
     * @param key      the data key to watch
     * @param listener callback when the value changes
     */
    public void subscribe(String key, Consumer<SyncEvent> listener) {
        subscribers.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>()).add(listener);
        logger.debug("Subscriber added for key: {}", key);
    }

    /**
     * Subscribe to all changes (use "*" as key).
     */
    public void subscribeAll(Consumer<SyncEvent> listener) {
        subscribe("*", listener);
    }

    /**
     * Unsubscribe a listener from a key.
     */
    public void unsubscribe(String key, Consumer<SyncEvent> listener) {
        List<Consumer<SyncEvent>> listeners = subscribers.get(key);
        if (listeners != null) {
            listeners.remove(listener);
        }
    }

    /**
     * Get the event log.
     */
    public List<SyncEvent> getEventLog() {
        return new ArrayList<>(eventLog);
    }

    /**
     * Get events for a specific key.
     */
    public List<SyncEvent> getEventsForKey(String key) {
        List<SyncEvent> filtered = new ArrayList<>();
        for (SyncEvent event : eventLog) {
            if (event.getKey().equals(key)) {
                filtered.add(event);
            }
        }
        return filtered;
    }

    /**
     * Get the number of items in the data store.
     */
    public int size() {
        return dataStore.size();
    }

    /**
     * Clear all data and notify subscribers.
     */
    public void clear() {
        for (String key : new ArrayList<>(dataStore.keySet())) {
            remove(key);
        }
    }

    private void notifySubscribers(String key, SyncEvent event) {
        // Notify key-specific subscribers
        List<Consumer<SyncEvent>> keyListeners = subscribers.get(key);
        if (keyListeners != null) {
            for (Consumer<SyncEvent> listener : keyListeners) {
                try {
                    listener.accept(event);
                } catch (Exception e) {
                    logger.warn("Subscriber error for key {}: {}", key, e.getMessage());
                }
            }
        }

        // Notify wildcard subscribers
        List<Consumer<SyncEvent>> allListeners = subscribers.get("*");
        if (allListeners != null) {
            for (Consumer<SyncEvent> listener : allListeners) {
                try {
                    listener.accept(event);
                } catch (Exception e) {
                    logger.warn("Wildcard subscriber error: {}", e.getMessage());
                }
            }
        }
    }

    private void recordEvent(SyncEvent event) {
        eventLog.add(event);
        // Trim event log if it exceeds max size
        while (eventLog.size() > maxEventLogSize) {
            eventLog.remove(0);
        }
    }

    @Override
    public void close() {
        dataStore.clear();
        subscribers.clear();
        eventLog.clear();
    }

    /**
     * Represents a data synchronization event.
     */
    public static class SyncEvent {
        public enum EventType { CREATED, UPDATED, DELETED }

        private final EventType type;
        private final String key;
        private final Object newValue;
        private final Object previousValue;
        private final long timestamp;

        public SyncEvent(EventType type, String key, Object newValue, Object previousValue, long timestamp) {
            this.type = type;
            this.key = key;
            this.newValue = newValue;
            this.previousValue = previousValue;
            this.timestamp = timestamp;
        }

        public EventType getType() { return type; }
        public String getKey() { return key; }
        public Object getNewValue() { return newValue; }
        public Object getPreviousValue() { return previousValue; }
        public long getTimestamp() { return timestamp; }

        @Override
        public String toString() {
            return String.format("SyncEvent[%s: %s = %s]", type, key, newValue);
        }
    }
}
