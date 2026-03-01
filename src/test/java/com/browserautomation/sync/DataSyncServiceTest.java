package com.browserautomation.sync;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for DataSyncService.
 */
class DataSyncServiceTest {

    private DataSyncService service;

    @BeforeEach
    void setUp() {
        service = new DataSyncService();
    }

    @AfterEach
    void tearDown() {
        service.close();
    }

    @Test
    void testDefaultConstructor() {
        assertNotNull(service);
        assertEquals(0, service.size());
    }

    @Test
    void testCustomMaxEventLogSize() {
        DataSyncService custom = new DataSyncService(5);
        assertNotNull(custom);
        custom.close();
    }

    @Test
    void testPutAndGet() {
        service.put("key1", "value1");
        assertEquals("value1", service.get("key1"));
    }

    @Test
    void testPutOverwrite() {
        service.put("key1", "value1");
        service.put("key1", "value2");
        assertEquals("value2", service.get("key1"));
    }

    @Test
    void testGetNonExistentKey() {
        assertNull(service.get("missing"));
    }

    @Test
    void testGetTyped() {
        service.put("count", 42);
        Integer val = service.get("count", Integer.class);
        assertEquals(42, val);
    }

    @Test
    void testGetTypedNull() {
        assertNull(service.get("missing", String.class));
    }

    @Test
    void testGetTypedWrongType() {
        service.put("key", "string-value");
        assertThrows(ClassCastException.class, () -> service.get("key", Integer.class));
    }

    @Test
    void testGetOrDefault() {
        assertEquals("default", service.getOrDefault("missing", "default"));
        service.put("key", "actual");
        assertEquals("actual", service.getOrDefault("key", "default"));
    }

    @Test
    void testRemove() {
        service.put("key", "value");
        Object removed = service.remove("key");
        assertEquals("value", removed);
        assertNull(service.get("key"));
    }

    @Test
    void testRemoveNonExistent() {
        assertNull(service.remove("missing"));
    }

    @Test
    void testContainsKey() {
        assertFalse(service.containsKey("key"));
        service.put("key", "value");
        assertTrue(service.containsKey("key"));
    }

    @Test
    void testKeys() {
        service.put("a", 1);
        service.put("b", 2);
        assertTrue(service.keys().contains("a"));
        assertTrue(service.keys().contains("b"));
    }

    @Test
    void testGetAll() {
        service.put("x", 10);
        service.put("y", 20);
        var all = service.getAll();
        assertEquals(2, all.size());
        assertEquals(10, all.get("x"));
    }

    @Test
    void testSize() {
        assertEquals(0, service.size());
        service.put("a", 1);
        assertEquals(1, service.size());
        service.put("b", 2);
        assertEquals(2, service.size());
    }

    @Test
    void testClear() {
        service.put("a", 1);
        service.put("b", 2);
        service.clear();
        assertEquals(0, service.size());
    }

    @Test
    void testSubscribeReceivesEvents() {
        List<DataSyncService.SyncEvent> received = new ArrayList<>();
        service.subscribe("key1", received::add);
        service.put("key1", "hello");
        assertEquals(1, received.size());
        assertEquals(DataSyncService.SyncEvent.EventType.CREATED, received.get(0).getType());
        assertEquals("key1", received.get(0).getKey());
    }

    @Test
    void testSubscribeUpdateEvent() {
        List<DataSyncService.SyncEvent> received = new ArrayList<>();
        service.put("key1", "old");
        service.subscribe("key1", received::add);
        service.put("key1", "new");
        assertEquals(1, received.size());
        assertEquals(DataSyncService.SyncEvent.EventType.UPDATED, received.get(0).getType());
        assertEquals("old", received.get(0).getPreviousValue());
        assertEquals("new", received.get(0).getNewValue());
    }

    @Test
    void testSubscribeDeleteEvent() {
        List<DataSyncService.SyncEvent> received = new ArrayList<>();
        service.put("key1", "value");
        service.subscribe("key1", received::add);
        service.remove("key1");
        assertEquals(1, received.size());
        assertEquals(DataSyncService.SyncEvent.EventType.DELETED, received.get(0).getType());
    }

    @Test
    void testSubscribeAll() {
        List<DataSyncService.SyncEvent> received = new ArrayList<>();
        service.subscribeAll(received::add);
        service.put("a", 1);
        service.put("b", 2);
        assertEquals(2, received.size());
    }

    @Test
    void testUnsubscribe() {
        List<DataSyncService.SyncEvent> received = new ArrayList<>();
        var listener = (java.util.function.Consumer<DataSyncService.SyncEvent>) received::add;
        service.subscribe("key", listener);
        service.put("key", "v1");
        assertEquals(1, received.size());
        service.unsubscribe("key", listener);
        service.put("key", "v2");
        assertEquals(1, received.size()); // no additional events
    }

    @Test
    void testEventLog() {
        service.put("a", 1);
        service.put("b", 2);
        service.put("a", 3);
        List<DataSyncService.SyncEvent> log = service.getEventLog();
        assertEquals(3, log.size());
    }

    @Test
    void testEventsForKey() {
        service.put("a", 1);
        service.put("b", 2);
        service.put("a", 3);
        List<DataSyncService.SyncEvent> aEvents = service.getEventsForKey("a");
        assertEquals(2, aEvents.size());
    }

    @Test
    void testSyncEventToString() {
        DataSyncService.SyncEvent event = new DataSyncService.SyncEvent(
                DataSyncService.SyncEvent.EventType.CREATED, "key", "val", null, System.currentTimeMillis());
        String str = event.toString();
        assertTrue(str.contains("CREATED"));
        assertTrue(str.contains("key"));
    }
}
