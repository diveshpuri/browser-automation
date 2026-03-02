package com.browserautomation.event;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class EventBusTest {

    private EventBus eventBus;

    @BeforeEach
    void setUp() {
        eventBus = new EventBus(100);
    }

    @AfterEach
    void tearDown() {
        eventBus.shutdown();
    }

    @Test
    void testSubscribeAndDispatch() {
        List<BrowserEvents.NavigateToUrlEvent> received = new ArrayList<>();
        eventBus.subscribe(BrowserEvents.NavigateToUrlEvent.class, received::add);

        eventBus.dispatch(new BrowserEvents.NavigateToUrlEvent("https://example.com"));

        assertEquals(1, received.size());
        assertEquals("https://example.com", received.get(0).getUrl());
    }

    @Test
    void testMultipleSubscribers() {
        AtomicInteger count = new AtomicInteger(0);
        eventBus.subscribe(BrowserEvents.ClickElementEvent.class, e -> count.incrementAndGet());
        eventBus.subscribe(BrowserEvents.ClickElementEvent.class, e -> count.incrementAndGet());

        eventBus.dispatch(new BrowserEvents.ClickElementEvent(5));

        assertEquals(2, count.get());
    }

    @Test
    void testWildcardSubscriber() {
        List<BrowserEvent> received = new ArrayList<>();
        eventBus.subscribe("*", received::add);

        eventBus.dispatch(new BrowserEvents.NavigateToUrlEvent("https://example.com"));
        eventBus.dispatch(new BrowserEvents.ClickElementEvent(1));

        assertEquals(2, received.size());
    }

    @Test
    void testEventHistory() {
        eventBus.dispatch(new BrowserEvents.NavigateToUrlEvent("https://example.com"));
        eventBus.dispatch(new BrowserEvents.ClickElementEvent(1));
        eventBus.dispatch(new BrowserEvents.ScrollEvent(true, 500));

        List<BrowserEvent> recent = eventBus.getRecentEvents(2);
        assertEquals(2, recent.size());

        assertEquals(3, eventBus.getEventHistory().size());
    }

    @Test
    void testEventHistoryByType() {
        eventBus.dispatch(new BrowserEvents.NavigateToUrlEvent("https://a.com"));
        eventBus.dispatch(new BrowserEvents.ClickElementEvent(1));
        eventBus.dispatch(new BrowserEvents.NavigateToUrlEvent("https://b.com"));

        List<BrowserEvent> navEvents = eventBus.getEventsByType("NavigateToUrlEvent");
        assertEquals(2, navEvents.size());
    }

    @Test
    void testClearHistory() {
        eventBus.dispatch(new BrowserEvents.NavigateToUrlEvent("https://example.com"));
        assertEquals(1, eventBus.getEventHistory().size());

        eventBus.clearHistory();
        assertEquals(0, eventBus.getEventHistory().size());
    }

    @Test
    void testHasSubscribers() {
        assertFalse(eventBus.hasSubscribers("NavigateToUrlEvent"));
        eventBus.subscribe(BrowserEvents.NavigateToUrlEvent.class, e -> {});
        assertTrue(eventBus.hasSubscribers("NavigateToUrlEvent"));
    }

    @Test
    void testSubscriberCount() {
        assertEquals(0, eventBus.getSubscriberCount("ClickElementEvent"));
        eventBus.subscribe(BrowserEvents.ClickElementEvent.class, e -> {});
        eventBus.subscribe(BrowserEvents.ClickElementEvent.class, e -> {});
        assertEquals(2, eventBus.getSubscriberCount("ClickElementEvent"));
    }

    @Test
    void testAsyncDispatch() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        eventBus.subscribe(BrowserEvents.NavigateToUrlEvent.class, e -> latch.countDown());

        eventBus.dispatchAsync(new BrowserEvents.NavigateToUrlEvent("https://example.com"));

        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testEventHandlerException() {
        eventBus.subscribe(BrowserEvents.ClickElementEvent.class, e -> {
            throw new RuntimeException("Test error");
        });

        // Should not throw - exceptions are caught
        assertDoesNotThrow(() -> eventBus.dispatch(new BrowserEvents.ClickElementEvent(1)));
    }

    @Test
    void testMaxHistorySize() {
        EventBus smallBus = new EventBus(3);
        try {
            for (int i = 0; i < 5; i++) {
                smallBus.dispatch(new BrowserEvents.ClickElementEvent(i));
            }
            assertTrue(smallBus.getEventHistory().size() <= 3);
        } finally {
            smallBus.shutdown();
        }
    }

    @Test
    void testUnsubscribe() {
        AtomicInteger count = new AtomicInteger(0);
        java.util.function.Consumer<BrowserEvents.ClickElementEvent> handler = e -> count.incrementAndGet();
        eventBus.subscribe(BrowserEvents.ClickElementEvent.class, handler);

        eventBus.dispatch(new BrowserEvents.ClickElementEvent(1));
        assertEquals(1, count.get());

        eventBus.unsubscribe(BrowserEvents.ClickElementEvent.class, handler);
        eventBus.dispatch(new BrowserEvents.ClickElementEvent(2));
        assertEquals(1, count.get());
    }
}
