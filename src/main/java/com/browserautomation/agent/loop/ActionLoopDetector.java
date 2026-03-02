package com.browserautomation.agent.loop;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/**
 * Detects repetitive action loops in agent execution using page fingerprinting
 * and rolling window action similarity tracking.
 *
 * (URL + element_count + text_hash) and rolling window action similarity.
 */
public class ActionLoopDetector {

    private static final Logger logger = LoggerFactory.getLogger(ActionLoopDetector.class);

    private final LoopDetectorConfig config;
    private final LinkedList<ActionRecord> actionHistory = new LinkedList<>();
    private final LinkedList<PageFingerprint> fingerprintHistory = new LinkedList<>();
    private int loopDetectionCount;

    public ActionLoopDetector() {
        this(new LoopDetectorConfig());
    }

    public ActionLoopDetector(LoopDetectorConfig config) {
        this.config = config;
    }

    /**
     * Record an action and check for loops.
     * Returns a LoopDetectionResult indicating if a loop was detected.
     */
    public LoopDetectionResult recordAction(String actionType, String actionDetails,
                                             String pageUrl, int elementCount, String pageTextSnippet) {
        // Create page fingerprint
        PageFingerprint fingerprint = new PageFingerprint(pageUrl, elementCount, hashText(pageTextSnippet));
        fingerprintHistory.addLast(fingerprint);
        if (fingerprintHistory.size() > config.getMaxHistorySize()) {
            fingerprintHistory.removeFirst();
        }

        // Record action
        ActionRecord record = new ActionRecord(actionType, actionDetails, fingerprint);
        actionHistory.addLast(record);
        if (actionHistory.size() > config.getMaxHistorySize()) {
            actionHistory.removeFirst();
        }

        // Check for action repetition loop
        boolean actionLoop = checkActionLoop();

        // Check for page fingerprint loop (same page state repeating)
        boolean fingerprintLoop = checkFingerprintLoop();

        if (actionLoop || fingerprintLoop) {
            loopDetectionCount++;
            String reason = actionLoop ? "Repeated actions detected" : "Same page state repeating";
            logger.warn("Loop detected (count={}): {}", loopDetectionCount, reason);
            return new LoopDetectionResult(true, loopDetectionCount, reason, generateNudge());
        }

        return new LoopDetectionResult(false, 0, null, null);
    }

    /**
     * Check if recent actions form a repeating pattern in a rolling window.
     */
    private boolean checkActionLoop() {
        if (actionHistory.size() < config.getMinWindowSize()) return false;

        int windowSize = Math.min(config.getRollingWindowSize(), actionHistory.size());
        List<ActionRecord> window = new ArrayList<>(
                actionHistory.subList(actionHistory.size() - windowSize, actionHistory.size()));

        // Count identical consecutive actions
        int consecutiveIdentical = 0;
        for (int i = window.size() - 1; i > 0; i--) {
            if (window.get(i).isSimilarTo(window.get(i - 1))) {
                consecutiveIdentical++;
            } else {
                break;
            }
        }

        if (consecutiveIdentical >= config.getMaxConsecutiveIdentical()) {
            return true;
        }

        // Check for repeating patterns (e.g., A-B-A-B)
        return checkRepeatingPattern(window);
    }

    /**
     * Check for repeating patterns in the action window (A-B-A-B, A-B-C-A-B-C, etc.)
     */
    private boolean checkRepeatingPattern(List<ActionRecord> window) {
        int maxPatternLength = window.size() / 2;
        for (int patternLen = 1; patternLen <= maxPatternLength; patternLen++) {
            int repetitions = 0;
            boolean patternMatch = true;

            for (int i = window.size() - 1; i >= patternLen; i--) {
                if (window.get(i).isSimilarTo(window.get(i - patternLen))) {
                    repetitions++;
                } else {
                    patternMatch = false;
                    break;
                }
                if (repetitions >= config.getMinPatternRepetitions() * patternLen) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Check if page fingerprints are repeating (stuck on same page state).
     */
    private boolean checkFingerprintLoop() {
        if (fingerprintHistory.size() < config.getMinWindowSize()) return false;

        int windowSize = Math.min(config.getRollingWindowSize(), fingerprintHistory.size());
        List<PageFingerprint> window = new ArrayList<>(
                fingerprintHistory.subList(fingerprintHistory.size() - windowSize, fingerprintHistory.size()));

        // Check if all recent fingerprints are identical
        PageFingerprint latest = window.get(window.size() - 1);
        int identicalCount = 0;
        for (int i = window.size() - 2; i >= 0; i--) {
            if (window.get(i).equals(latest)) {
                identicalCount++;
            } else {
                break;
            }
        }

        return identicalCount >= config.getMaxConsecutiveIdentical();
    }

    /**
     * Generate a nudge message to help the agent break out of a loop.
     */
    private String generateNudge() {
        List<String> nudges = List.of(
                "You seem to be repeating the same actions. Try a different approach.",
                "The page state hasn't changed. Consider trying a different element or strategy.",
                "Loop detected. Try scrolling, navigating to a different page, or using a different action.",
                "Your recent actions are repetitive. Think about what's different you could try.",
                "Consider using a completely different strategy to accomplish your goal."
        );
        return nudges.get(loopDetectionCount % nudges.size());
    }

    /**
     * Hash text content for fingerprinting.
     */
    private String hashText(String text) {
        if (text == null || text.isEmpty()) return "";
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(text.substring(0, Math.min(text.length(), 500))
                    .getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (int i = 0; i < Math.min(8, hash.length); i++) {
                hexString.append(String.format("%02x", hash[i]));
            }
            return hexString.toString();
        } catch (Exception e) {
            return String.valueOf(text.hashCode());
        }
    }

    public void reset() {
        actionHistory.clear();
        fingerprintHistory.clear();
        loopDetectionCount = 0;
    }

    public int getLoopDetectionCount() { return loopDetectionCount; }
    public int getActionHistorySize() { return actionHistory.size(); }

    /**
     * A recorded action with its associated page fingerprint.
     */
    public static class ActionRecord {
        private final String actionType;
        private final String actionDetails;
        private final PageFingerprint fingerprint;

        public ActionRecord(String actionType, String actionDetails, PageFingerprint fingerprint) {
            this.actionType = actionType;
            this.actionDetails = actionDetails;
            this.fingerprint = fingerprint;
        }

        public boolean isSimilarTo(ActionRecord other) {
            return actionType.equals(other.actionType) &&
                    Objects.equals(actionDetails, other.actionDetails);
        }

        public String getActionType() { return actionType; }
        public String getActionDetails() { return actionDetails; }
        public PageFingerprint getFingerprint() { return fingerprint; }
    }

    /**
     * Page fingerprint combining URL, element count, and text hash.
     */
    public record PageFingerprint(String url, int elementCount, String textHash) {}

    /**
     * Result of loop detection check.
     */
    public record LoopDetectionResult(boolean loopDetected, int loopCount, String reason, String nudge) {}

    /**
     * Configuration for loop detection.
     */
    public static class LoopDetectorConfig {
        private int rollingWindowSize = 10;
        private int maxConsecutiveIdentical = 3;
        private int minPatternRepetitions = 2;
        private int minWindowSize = 4;
        private int maxHistorySize = 50;

        public LoopDetectorConfig rollingWindowSize(int v) { rollingWindowSize = v; return this; }
        public LoopDetectorConfig maxConsecutiveIdentical(int v) { maxConsecutiveIdentical = v; return this; }
        public LoopDetectorConfig minPatternRepetitions(int v) { minPatternRepetitions = v; return this; }
        public LoopDetectorConfig minWindowSize(int v) { minWindowSize = v; return this; }
        public LoopDetectorConfig maxHistorySize(int v) { maxHistorySize = v; return this; }

        public int getRollingWindowSize() { return rollingWindowSize; }
        public int getMaxConsecutiveIdentical() { return maxConsecutiveIdentical; }
        public int getMinPatternRepetitions() { return minPatternRepetitions; }
        public int getMinWindowSize() { return minWindowSize; }
        public int getMaxHistorySize() { return maxHistorySize; }
    }
}
