package com.browserautomation.browser;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for VideoRecorder.
 */
class VideoRecorderTest {

    @Test
    void testDefaultConstructor() {
        VideoRecorder recorder = new VideoRecorder();
        assertFalse(recorder.isRecording());
        assertTrue(recorder.getRecordedVideos().isEmpty());
        assertEquals(1280, recorder.getVideoWidth());
        assertEquals(720, recorder.getVideoHeight());
    }

    @Test
    void testCustomConstructor(@TempDir Path tempDir) {
        VideoRecorder recorder = new VideoRecorder(tempDir, 1920, 1080);
        assertEquals(tempDir, recorder.getOutputDirectory());
        assertEquals(1920, recorder.getVideoWidth());
        assertEquals(1080, recorder.getVideoHeight());
    }

    @Test
    void testSetters() {
        VideoRecorder recorder = new VideoRecorder();
        recorder.setVideoWidth(800);
        recorder.setVideoHeight(600);
        assertEquals(800, recorder.getVideoWidth());
        assertEquals(600, recorder.getVideoHeight());
    }

    @Test
    void testGetRecordingContextOptions(@TempDir Path tempDir) {
        VideoRecorder recorder = new VideoRecorder(tempDir, 1280, 720);
        var options = recorder.getRecordingContextOptions();
        assertNotNull(options);
        assertTrue(recorder.isRecording());
    }

    @Test
    void testApplyRecording(@TempDir Path tempDir) {
        VideoRecorder recorder = new VideoRecorder(tempDir, 1280, 720);
        var options = new com.microsoft.playwright.Browser.NewContextOptions();
        var result = recorder.applyRecording(options);
        assertNotNull(result);
        assertTrue(recorder.isRecording());
    }
}
