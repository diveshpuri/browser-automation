package com.browserautomation.screenshot;

import com.browserautomation.browser.BrowserSession;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Dedicated screenshot service for browser automation.
 * Equivalent to browser-use's screenshots module.
 *
 * Provides advanced screenshot capabilities including full-page captures,
 * element-specific screenshots, comparison, and organized storage.
 */
public class ScreenshotService {

    private static final Logger logger = LoggerFactory.getLogger(ScreenshotService.class);
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");

    private final Path outputDirectory;
    private final List<ScreenshotRecord> history;

    public ScreenshotService() {
        this(null);
    }

    public ScreenshotService(Path outputDirectory) {
        if (outputDirectory == null) {
            try {
                this.outputDirectory = Files.createTempDirectory("ba-screenshots-");
            } catch (IOException e) {
                throw new RuntimeException("Failed to create screenshots directory", e);
            }
        } else {
            this.outputDirectory = outputDirectory;
            try {
                Files.createDirectories(outputDirectory);
            } catch (IOException e) {
                throw new RuntimeException("Failed to create screenshots directory: " + outputDirectory, e);
            }
        }
        this.history = new ArrayList<>();
    }

    /**
     * Take a viewport screenshot.
     *
     * @param session the browser session
     * @return the screenshot record
     */
    public ScreenshotRecord captureViewport(BrowserSession session) {
        return captureViewport(session, null);
    }

    /**
     * Take a viewport screenshot with a label.
     */
    public ScreenshotRecord captureViewport(BrowserSession session, String label) {
        byte[] data = session.getCurrentPage().screenshot(
                new Page.ScreenshotOptions().setFullPage(false));
        return saveScreenshot(data, "viewport", label);
    }

    /**
     * Take a full-page screenshot.
     */
    public ScreenshotRecord captureFullPage(BrowserSession session) {
        return captureFullPage(session, null);
    }

    /**
     * Take a full-page screenshot with a label.
     */
    public ScreenshotRecord captureFullPage(BrowserSession session, String label) {
        byte[] data = session.getCurrentPage().screenshot(
                new Page.ScreenshotOptions().setFullPage(true));
        return saveScreenshot(data, "fullpage", label);
    }

    /**
     * Capture a specific element by CSS selector.
     */
    public ScreenshotRecord captureElement(BrowserSession session, String selector) {
        return captureElement(session, selector, null);
    }

    /**
     * Capture a specific element by CSS selector with a label.
     */
    public ScreenshotRecord captureElement(BrowserSession session, String selector, String label) {
        Locator locator = session.getCurrentPage().locator(selector).first();
        byte[] data = locator.screenshot();
        return saveScreenshot(data, "element", label);
    }

    /**
     * Take a screenshot and return as base64 string.
     */
    public String captureAsBase64(BrowserSession session, boolean fullPage) {
        byte[] data = session.getCurrentPage().screenshot(
                new Page.ScreenshotOptions().setFullPage(fullPage));
        return Base64.getEncoder().encodeToString(data);
    }

    /**
     * Take a screenshot and save it to a specific file.
     */
    public ScreenshotRecord captureToFile(BrowserSession session, Path filePath, boolean fullPage) {
        byte[] data = session.getCurrentPage().screenshot(
                new Page.ScreenshotOptions().setFullPage(fullPage));
        try {
            Files.createDirectories(filePath.getParent());
            Files.write(filePath, data);
            ScreenshotRecord record = new ScreenshotRecord(
                    filePath, data.length, fullPage ? "fullpage" : "viewport", null,
                    session.getCurrentPage().url(), LocalDateTime.now());
            history.add(record);
            return record;
        } catch (IOException e) {
            throw new RuntimeException("Failed to save screenshot: " + e.getMessage(), e);
        }
    }

    /**
     * Compare two screenshots and return the difference percentage.
     * Uses simple pixel-level comparison.
     *
     * @param screenshot1 first screenshot data
     * @param screenshot2 second screenshot data
     * @return difference percentage (0.0 = identical, 1.0 = completely different)
     */
    public double compareScreenshots(byte[] screenshot1, byte[] screenshot2) {
        if (screenshot1 == null || screenshot2 == null) return 1.0;
        if (screenshot1.length == 0 || screenshot2.length == 0) return 1.0;

        // Simple byte-level comparison
        int minLen = Math.min(screenshot1.length, screenshot2.length);
        int maxLen = Math.max(screenshot1.length, screenshot2.length);
        int diffCount = maxLen - minLen; // Bytes in longer but not shorter

        for (int i = 0; i < minLen; i++) {
            if (screenshot1[i] != screenshot2[i]) {
                diffCount++;
            }
        }

        return (double) diffCount / maxLen;
    }

    private ScreenshotRecord saveScreenshot(byte[] data, String type, String label) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String filename = type + "_" + timestamp + ".png";
        Path filePath = outputDirectory.resolve(filename);

        try {
            Files.write(filePath, data);
            ScreenshotRecord record = new ScreenshotRecord(filePath, data.length, type, label, null, LocalDateTime.now());
            history.add(record);
            logger.debug("Screenshot saved: {} ({} bytes)", filePath, data.length);
            return record;
        } catch (IOException e) {
            throw new RuntimeException("Failed to save screenshot: " + e.getMessage(), e);
        }
    }

    /**
     * Get all screenshot records.
     */
    public List<ScreenshotRecord> getHistory() {
        return new ArrayList<>(history);
    }

    /**
     * Get the output directory.
     */
    public Path getOutputDirectory() {
        return outputDirectory;
    }

    /**
     * Record of a captured screenshot.
     */
    public static class ScreenshotRecord {
        private final Path filePath;
        private final long sizeBytes;
        private final String type;
        private final String label;
        private final String pageUrl;
        private final LocalDateTime timestamp;

        public ScreenshotRecord(Path filePath, long sizeBytes, String type, String label,
                                String pageUrl, LocalDateTime timestamp) {
            this.filePath = filePath;
            this.sizeBytes = sizeBytes;
            this.type = type;
            this.label = label;
            this.pageUrl = pageUrl;
            this.timestamp = timestamp;
        }

        public Path getFilePath() { return filePath; }
        public long getSizeBytes() { return sizeBytes; }
        public String getType() { return type; }
        public String getLabel() { return label; }
        public String getPageUrl() { return pageUrl; }
        public LocalDateTime getTimestamp() { return timestamp; }

        @Override
        public String toString() {
            return String.format("Screenshot[%s, %s, %d bytes, %s]",
                    type, label != null ? label : "unlabeled", sizeBytes, filePath.getFileName());
        }
    }
}
