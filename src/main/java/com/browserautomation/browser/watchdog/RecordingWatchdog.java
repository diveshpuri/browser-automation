package com.browserautomation.browser.watchdog;

import com.browserautomation.browser.BrowserSession;
import com.browserautomation.event.BrowserEvents;
import com.browserautomation.event.EventBus;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Watchdog that manages browser-level screen recording of automation sessions.
 * Records actions and their timestamps for replay and debugging.
 */
public class RecordingWatchdog extends BaseWatchdog {

    private final BrowserSession session;
    private final Path recordingDirectory;
    private final List<RecordedAction> recordedActions = new ArrayList<>();
    private volatile boolean recording;
    private Instant recordingStartTime;

    public RecordingWatchdog(EventBus eventBus, BrowserSession session, Path recordingDirectory) {
        super(eventBus);
        this.session = session;
        this.recordingDirectory = recordingDirectory;
    }

    @Override
    public String getWatchdogName() { return "recording"; }

    @Override
    protected void subscribeToEvents() {
        // Record all browser action events
        eventBus.subscribe(BrowserEvents.ClickElementEvent.class, event ->
                recordAction("click", "element=" + event.getElementIndex()));
        eventBus.subscribe(BrowserEvents.TypeTextEvent.class, event ->
                recordAction("type", "element=" + event.getElementIndex() + ", text=" + event.getText()));
        eventBus.subscribe(BrowserEvents.NavigateToUrlEvent.class, event ->
                recordAction("navigate", "url=" + event.getUrl()));
        eventBus.subscribe(BrowserEvents.ScrollEvent.class, event ->
                recordAction("scroll", (event.isDown() ? "down" : "up") + " " + event.getPixels() + "px"));
        eventBus.subscribe(BrowserEvents.SendKeysEvent.class, event ->
                recordAction("sendKeys", "keys=" + event.getKeys()));
        eventBus.subscribe(BrowserEvents.SwitchTabEvent.class, event ->
                recordAction("switchTab", "tab=" + event.getTabIndex()));
    }

    public void startRecording() {
        recording = true;
        recordingStartTime = Instant.now();
        recordedActions.clear();
        logger.info("Recording started");
    }

    public void stopRecording() {
        recording = false;
        logger.info("Recording stopped ({} actions recorded)", recordedActions.size());
    }

    private void recordAction(String actionType, String details) {
        if (!recording) return;
        long elapsedMs = recordingStartTime != null ?
                Instant.now().toEpochMilli() - recordingStartTime.toEpochMilli() : 0;
        recordedActions.add(new RecordedAction(actionType, details, elapsedMs));
    }

    public List<RecordedAction> getRecordedActions() { return List.copyOf(recordedActions); }
    public boolean isRecording() { return recording; }
    public Path getRecordingDirectory() { return recordingDirectory; }

    public static class RecordedAction {
        private final String actionType;
        private final String details;
        private final long elapsedMs;

        public RecordedAction(String actionType, String details, long elapsedMs) {
            this.actionType = actionType;
            this.details = details;
            this.elapsedMs = elapsedMs;
        }

        public String getActionType() { return actionType; }
        public String getDetails() { return details; }
        public long getElapsedMs() { return elapsedMs; }

        @Override
        public String toString() {
            return String.format("[%dms] %s: %s", elapsedMs, actionType, details);
        }
    }
}
