package com.browserautomation.agent;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for GifRecorder.
 */
class GifRecorderTest {

    @Test
    void testDefaultConstructor() {
        GifRecorder recorder = new GifRecorder();
        assertFalse(recorder.isRecording());
        assertEquals(0, recorder.getFrameCount());
    }

    @Test
    void testCustomConstructor() {
        GifRecorder recorder = new GifRecorder(250, 300);
        assertFalse(recorder.isRecording());
        assertEquals(0, recorder.getFrameCount());
    }

    @Test
    void testStopWithoutStartDoesNotThrow() {
        GifRecorder recorder = new GifRecorder();
        assertDoesNotThrow(() -> recorder.stopRecording());
    }

    @Test
    void testCloseStopsRecording() {
        GifRecorder recorder = new GifRecorder();
        assertDoesNotThrow(() -> recorder.close());
        assertFalse(recorder.isRecording());
    }

    @Test
    void testSaveGifWithNoFramesThrows() {
        GifRecorder recorder = new GifRecorder();
        assertThrows(Exception.class, () -> recorder.saveGifToTemp());
    }
}
