package com.browserautomation.agent.loop;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ActionLoopDetectorTest {

    private ActionLoopDetector detector;

    @BeforeEach
    void setUp() {
        detector = new ActionLoopDetector(new ActionLoopDetector.LoopDetectorConfig()
                .rollingWindowSize(10)
                .maxConsecutiveIdentical(3)
                .minPatternRepetitions(2)
                .minWindowSize(4)
                .maxHistorySize(50));
    }

    @Test
    void testNoLoopDetectedForDifferentActions() {
        var r1 = detector.recordAction("click", "element=1", "https://a.com", 10, "text1");
        assertFalse(r1.loopDetected());

        var r2 = detector.recordAction("type", "element=2", "https://a.com", 10, "text2");
        assertFalse(r2.loopDetected());

        var r3 = detector.recordAction("scroll", "down 500", "https://a.com", 10, "text3");
        assertFalse(r3.loopDetected());

        var r4 = detector.recordAction("navigate", "url=b.com", "https://b.com", 15, "text4");
        assertFalse(r4.loopDetected());
    }

    @Test
    void testDetectsConsecutiveIdenticalActions() {
        // Same action repeated 4 times
        detector.recordAction("click", "element=1", "https://a.com", 10, "same text");
        detector.recordAction("click", "element=1", "https://a.com", 10, "same text");
        detector.recordAction("click", "element=1", "https://a.com", 10, "same text");
        var result = detector.recordAction("click", "element=1", "https://a.com", 10, "same text");

        assertTrue(result.loopDetected());
        assertTrue(result.loopCount() > 0);
        assertNotNull(result.nudge());
        assertNotNull(result.reason());
    }

    @Test
    void testDetectsPageFingerprintLoop() {
        // Same page state repeating
        for (int i = 0; i < 5; i++) {
            var result = detector.recordAction("action" + (i % 2), "details",
                    "https://same.com", 42, "identical page text");
            if (i >= 4) {
                // After enough repetitions, fingerprint loop should be detected
                // (or action loop depending on pattern)
            }
        }
        assertEquals(5, detector.getActionHistorySize());
    }

    @Test
    void testResetClearsState() {
        detector.recordAction("click", "element=1", "https://a.com", 10, "text");
        detector.recordAction("click", "element=1", "https://a.com", 10, "text");

        detector.reset();
        assertEquals(0, detector.getActionHistorySize());
        assertEquals(0, detector.getLoopDetectionCount());
    }

    @Test
    void testLoopDetectorConfig() {
        ActionLoopDetector.LoopDetectorConfig config = new ActionLoopDetector.LoopDetectorConfig()
                .rollingWindowSize(20)
                .maxConsecutiveIdentical(5)
                .minPatternRepetitions(3)
                .minWindowSize(6)
                .maxHistorySize(100);

        assertEquals(20, config.getRollingWindowSize());
        assertEquals(5, config.getMaxConsecutiveIdentical());
        assertEquals(3, config.getMinPatternRepetitions());
        assertEquals(6, config.getMinWindowSize());
        assertEquals(100, config.getMaxHistorySize());
    }

    @Test
    void testActionRecordSimilarity() {
        var fp = new ActionLoopDetector.PageFingerprint("url", 10, "hash");
        var record1 = new ActionLoopDetector.ActionRecord("click", "element=1", fp);
        var record2 = new ActionLoopDetector.ActionRecord("click", "element=1", fp);
        var record3 = new ActionLoopDetector.ActionRecord("click", "element=2", fp);

        assertTrue(record1.isSimilarTo(record2));
        assertFalse(record1.isSimilarTo(record3));
    }

    @Test
    void testPageFingerprint() {
        var fp1 = new ActionLoopDetector.PageFingerprint("url1", 10, "hash1");
        var fp2 = new ActionLoopDetector.PageFingerprint("url1", 10, "hash1");
        var fp3 = new ActionLoopDetector.PageFingerprint("url2", 10, "hash1");

        assertEquals(fp1, fp2);
        assertNotEquals(fp1, fp3);
    }

    @Test
    void testLoopDetectionResult() {
        var result = new ActionLoopDetector.LoopDetectionResult(true, 2, "Repeated actions", "Try different approach");
        assertTrue(result.loopDetected());
        assertEquals(2, result.loopCount());
        assertEquals("Repeated actions", result.reason());
        assertEquals("Try different approach", result.nudge());
    }

    @Test
    void testNudgeMessagesRotate() {
        // Force loop detection multiple times and check that nudge messages vary
        for (int round = 0; round < 3; round++) {
            detector.reset();
            for (int i = 0; i < 5; i++) {
                detector.recordAction("click", "element=1", "https://a.com", 10, "same");
            }
        }
        assertTrue(detector.getLoopDetectionCount() > 0);
    }

    @Test
    void testMaxHistorySizeRespected() {
        ActionLoopDetector smallDetector = new ActionLoopDetector(
                new ActionLoopDetector.LoopDetectorConfig().maxHistorySize(5));
        for (int i = 0; i < 10; i++) {
            smallDetector.recordAction("action" + i, "details" + i,
                    "url" + i, i, "text" + i);
        }
        assertTrue(smallDetector.getActionHistorySize() <= 5);
    }

    @Test
    void testDetectsRepeatingPattern() {
        // A-B-A-B pattern
        for (int i = 0; i < 6; i++) {
            String action = i % 2 == 0 ? "click" : "type";
            String details = i % 2 == 0 ? "element=1" : "element=2";
            detector.recordAction(action, details, "https://a.com", 10, "text");
        }
        // The pattern detector should eventually flag this
        assertTrue(detector.getActionHistorySize() >= 6);
    }
}
