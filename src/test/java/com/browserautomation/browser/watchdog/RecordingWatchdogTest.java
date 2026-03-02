package com.browserautomation.browser.watchdog;

import com.browserautomation.browser.BrowserSession;
import com.browserautomation.event.BrowserEvents;
import com.browserautomation.event.EventBus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RecordingWatchdogTest {

    private EventBus eventBus;
    private BrowserSession session;
    private RecordingWatchdog watchdog;

    @BeforeEach
    void setUp() {
        eventBus = new EventBus();
        session = mock(BrowserSession.class);
        when(session.isStarted()).thenReturn(false);
        watchdog = new RecordingWatchdog(eventBus, session, Path.of("/tmp/test-recordings"));
    }

    @AfterEach
    void tearDown() {
        watchdog.close();
        eventBus.shutdown();
    }

    @Test
    void testWatchdogName() {
        assertEquals("recording", watchdog.getWatchdogName());
    }

    @Test
    void testStartAndStopRecording() {
        watchdog.start();
        assertFalse(watchdog.isRecording());

        watchdog.startRecording();
        assertTrue(watchdog.isRecording());

        watchdog.stopRecording();
        assertFalse(watchdog.isRecording());
    }

    @Test
    void testRecordActionsWhenRecording() {
        watchdog.start();
        watchdog.startRecording();

        // Dispatch events that the watchdog subscribes to
        eventBus.dispatch(new BrowserEvents.ClickElementEvent(1));
        eventBus.dispatch(new BrowserEvents.NavigateToUrlEvent("https://example.com"));
        eventBus.dispatch(new BrowserEvents.ScrollEvent(true, 500));

        var actions = watchdog.getRecordedActions();
        assertEquals(3, actions.size());
        assertEquals("click", actions.get(0).getActionType());
        assertEquals("navigate", actions.get(1).getActionType());
        assertEquals("scroll", actions.get(2).getActionType());
    }

    @Test
    void testNoRecordingWhenNotStarted() {
        watchdog.start();
        // Not calling startRecording()

        eventBus.dispatch(new BrowserEvents.ClickElementEvent(1));

        assertTrue(watchdog.getRecordedActions().isEmpty());
    }

    @Test
    void testRecordedActionTimestamps() throws InterruptedException {
        watchdog.start();
        watchdog.startRecording();

        eventBus.dispatch(new BrowserEvents.ClickElementEvent(1));
        Thread.sleep(50);
        eventBus.dispatch(new BrowserEvents.ClickElementEvent(2));

        var actions = watchdog.getRecordedActions();
        assertEquals(2, actions.size());
        assertTrue(actions.get(1).getElapsedMs() >= actions.get(0).getElapsedMs());
    }

    @Test
    void testRecordedActionToString() {
        watchdog.start();
        watchdog.startRecording();

        eventBus.dispatch(new BrowserEvents.TypeTextEvent(3, "hello"));

        var actions = watchdog.getRecordedActions();
        assertEquals(1, actions.size());
        String str = actions.get(0).toString();
        assertTrue(str.contains("type"));
    }

    @Test
    void testGetRecordingDirectory() {
        assertEquals(Path.of("/tmp/test-recordings"), watchdog.getRecordingDirectory());
    }
}
