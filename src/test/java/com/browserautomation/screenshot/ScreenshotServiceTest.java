package com.browserautomation.screenshot;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ScreenshotService.
 */
class ScreenshotServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void testDefaultConstructorCreatesDirectory() {
        ScreenshotService service = new ScreenshotService();
        assertNotNull(service.getOutputDirectory());
        assertTrue(Files.exists(service.getOutputDirectory()));
    }

    @Test
    void testCustomDirectoryConstructor() {
        Path customDir = tempDir.resolve("screenshots");
        ScreenshotService service = new ScreenshotService(customDir);
        assertEquals(customDir, service.getOutputDirectory());
        assertTrue(Files.exists(customDir));
    }

    @Test
    void testHistoryStartsEmpty() {
        ScreenshotService service = new ScreenshotService(tempDir.resolve("ss"));
        assertTrue(service.getHistory().isEmpty());
    }

    @Test
    void testCompareScreenshotsIdentical() {
        ScreenshotService service = new ScreenshotService();
        byte[] data = new byte[]{1, 2, 3, 4, 5};
        double diff = service.compareScreenshots(data, data);
        assertEquals(0.0, diff, 0.001);
    }

    @Test
    void testCompareScreenshotsCompletelyDifferent() {
        ScreenshotService service = new ScreenshotService();
        byte[] data1 = new byte[]{0, 0, 0, 0};
        byte[] data2 = new byte[]{1, 1, 1, 1};
        double diff = service.compareScreenshots(data1, data2);
        assertEquals(1.0, diff, 0.001);
    }

    @Test
    void testCompareScreenshotsPartialDifference() {
        ScreenshotService service = new ScreenshotService();
        byte[] data1 = new byte[]{1, 2, 3, 4};
        byte[] data2 = new byte[]{1, 2, 5, 6};
        double diff = service.compareScreenshots(data1, data2);
        assertTrue(diff > 0.0 && diff < 1.0);
    }

    @Test
    void testCompareScreenshotsNullReturnsOne() {
        ScreenshotService service = new ScreenshotService();
        assertEquals(1.0, service.compareScreenshots(null, new byte[]{1}));
        assertEquals(1.0, service.compareScreenshots(new byte[]{1}, null));
    }

    @Test
    void testCompareScreenshotsEmptyReturnsOne() {
        ScreenshotService service = new ScreenshotService();
        assertEquals(1.0, service.compareScreenshots(new byte[0], new byte[]{1}));
        assertEquals(1.0, service.compareScreenshots(new byte[]{1}, new byte[0]));
    }

    @Test
    void testCompareScreenshotsDifferentLength() {
        ScreenshotService service = new ScreenshotService();
        byte[] data1 = new byte[]{1, 2, 3};
        byte[] data2 = new byte[]{1, 2, 3, 4, 5};
        double diff = service.compareScreenshots(data1, data2);
        assertTrue(diff > 0.0); // Different lengths means some difference
    }

    @Test
    void testScreenshotRecordProperties() {
        Path filePath = tempDir.resolve("test.png");
        LocalDateTime now = LocalDateTime.now();
        ScreenshotService.ScreenshotRecord record = new ScreenshotService.ScreenshotRecord(
                filePath, 1024, "viewport", "test-label", "https://example.com", now);

        assertEquals(filePath, record.getFilePath());
        assertEquals(1024, record.getSizeBytes());
        assertEquals("viewport", record.getType());
        assertEquals("test-label", record.getLabel());
        assertEquals("https://example.com", record.getPageUrl());
        assertEquals(now, record.getTimestamp());
    }

    @Test
    void testScreenshotRecordToString() {
        Path filePath = tempDir.resolve("test.png");
        ScreenshotService.ScreenshotRecord record = new ScreenshotService.ScreenshotRecord(
                filePath, 2048, "fullpage", "my-label", null, LocalDateTime.now());

        String str = record.toString();
        assertTrue(str.contains("fullpage"));
        assertTrue(str.contains("my-label"));
        assertTrue(str.contains("2048"));
    }

    @Test
    void testScreenshotRecordToStringNullLabel() {
        Path filePath = tempDir.resolve("test.png");
        ScreenshotService.ScreenshotRecord record = new ScreenshotService.ScreenshotRecord(
                filePath, 512, "element", null, null, LocalDateTime.now());

        String str = record.toString();
        assertTrue(str.contains("unlabeled"));
    }
}
