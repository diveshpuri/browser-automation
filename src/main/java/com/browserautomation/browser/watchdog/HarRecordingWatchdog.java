package com.browserautomation.browser.watchdog;

import com.browserautomation.browser.BrowserSession;
import com.browserautomation.event.BrowserEvents;
import com.browserautomation.event.EventBus;

import java.nio.file.Path;

/**
 * Watchdog that manages HAR (HTTP Archive) recording of network traffic.
 */
public class HarRecordingWatchdog extends BaseWatchdog {

    private final BrowserSession session;
    private final Path harFilePath;
    private volatile boolean recording;

    public HarRecordingWatchdog(EventBus eventBus, BrowserSession session, Path harFilePath) {
        super(eventBus);
        this.session = session;
        this.harFilePath = harFilePath;
    }

    @Override
    public String getWatchdogName() { return "har_recording"; }

    @Override
    protected void subscribeToEvents() {
        eventBus.subscribe(BrowserEvents.AgentStartedEvent.class, event -> {
            if (!recording) startRecording();
        });
        eventBus.subscribe(BrowserEvents.AgentCompletedEvent.class, event -> {
            if (recording) stopRecording();
        });
    }

    public void startRecording() {
        if (!session.isStarted()) return;
        recording = true;
        logger.info("HAR recording started: {}", harFilePath);
        dispatchEvent(new BrowserEvents.HarRecordingEvent("start", harFilePath.toString()));
    }

    public void stopRecording() {
        recording = false;
        logger.info("HAR recording stopped: {}", harFilePath);
        dispatchEvent(new BrowserEvents.HarRecordingEvent("stop", harFilePath.toString()));
    }

    public boolean isRecording() { return recording; }
    public Path getHarFilePath() { return harFilePath; }
}
