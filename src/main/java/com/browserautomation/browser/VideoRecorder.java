package com.browserautomation.browser;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Records browser automation sessions as video files.
 * Equivalent to browser-use's browser/video_recorder.py.
 *
 * Uses Playwright's built-in video recording capability to capture
 * the browser session as a WebM video file.
 */
public class VideoRecorder {

    private static final Logger logger = LoggerFactory.getLogger(VideoRecorder.class);

    private Path outputDirectory;
    private int videoWidth;
    private int videoHeight;
    private boolean recording;
    private final List<Path> recordedVideos;

    /**
     * Create a video recorder with default settings.
     */
    public VideoRecorder() {
        this(null, 1280, 720);
    }

    /**
     * Create a video recorder with custom settings.
     *
     * @param outputDirectory directory to save videos (null for temp dir)
     * @param videoWidth      width of the recorded video
     * @param videoHeight     height of the recorded video
     */
    public VideoRecorder(Path outputDirectory, int videoWidth, int videoHeight) {
        this.outputDirectory = outputDirectory;
        this.videoWidth = videoWidth;
        this.videoHeight = videoHeight;
        this.recording = false;
        this.recordedVideos = new ArrayList<>();
    }

    /**
     * Get context options configured for video recording.
     * Call this before creating a BrowserContext to enable recording.
     *
     * @return context options with video recording enabled
     */
    public Browser.NewContextOptions getRecordingContextOptions() {
        Path dir = getOrCreateOutputDirectory();
        Browser.NewContextOptions options = new Browser.NewContextOptions();
        options.setRecordVideoDir(dir);
        options.setRecordVideoSize(videoWidth, videoHeight);
        recording = true;
        logger.info("Video recording enabled: {}x{} -> {}", videoWidth, videoHeight, dir);
        return options;
    }

    /**
     * Apply video recording settings to existing context options.
     *
     * @param options existing context options to modify
     * @return the modified options
     */
    public Browser.NewContextOptions applyRecording(Browser.NewContextOptions options) {
        Path dir = getOrCreateOutputDirectory();
        options.setRecordVideoDir(dir);
        options.setRecordVideoSize(videoWidth, videoHeight);
        recording = true;
        logger.info("Video recording applied: {}x{} -> {}", videoWidth, videoHeight, dir);
        return options;
    }

    /**
     * Stop recording and collect the video file path.
     * Must be called after closing the BrowserContext.
     *
     * @param context the browser context that was recording
     * @return the path to the recorded video, or null if none
     */
    public Path stopRecording(BrowserContext context) {
        recording = false;
        try {
            // Playwright saves the video when the context/page closes
            var pages = context.pages();
            if (!pages.isEmpty()) {
                var video = pages.get(0).video();
                if (video != null) {
                    Path videoPath = video.path();
                    recordedVideos.add(videoPath);
                    logger.info("Video saved: {}", videoPath);
                    return videoPath;
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to get video path: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Save the video to a specific location.
     *
     * @param context    the browser context that recorded
     * @param targetPath the destination file path
     * @return the target path
     */
    public Path saveVideoAs(BrowserContext context, Path targetPath) {
        try {
            var pages = context.pages();
            if (!pages.isEmpty()) {
                var video = pages.get(0).video();
                if (video != null) {
                    video.saveAs(targetPath);
                    recordedVideos.add(targetPath);
                    logger.info("Video saved to: {}", targetPath);
                    return targetPath;
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to save video: {}", e.getMessage());
        }
        return null;
    }

    private Path getOrCreateOutputDirectory() {
        if (outputDirectory == null) {
            try {
                outputDirectory = Files.createTempDirectory("browser-automation-videos-");
            } catch (Exception e) {
                throw new RuntimeException("Failed to create video output directory", e);
            }
        }
        if (!Files.exists(outputDirectory)) {
            try {
                Files.createDirectories(outputDirectory);
            } catch (Exception e) {
                throw new RuntimeException("Failed to create video output directory: " + outputDirectory, e);
            }
        }
        return outputDirectory;
    }

    /**
     * Get all recorded video paths.
     */
    public List<Path> getRecordedVideos() {
        return new ArrayList<>(recordedVideos);
    }

    /**
     * Check if currently recording.
     */
    public boolean isRecording() {
        return recording;
    }

    // Getters and setters
    public Path getOutputDirectory() { return outputDirectory; }
    public void setOutputDirectory(Path dir) { this.outputDirectory = dir; }
    public int getVideoWidth() { return videoWidth; }
    public void setVideoWidth(int w) { this.videoWidth = w; }
    public int getVideoHeight() { return videoHeight; }
    public void setVideoHeight(int h) { this.videoHeight = h; }
}
