package com.browserautomation.agent;

import com.browserautomation.browser.BrowserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Records browser automation sessions as animated GIF files.
 * Equivalent to browser-use's agent/gif.py.
 *
 * Captures screenshots at regular intervals and assembles them
 * into an animated GIF when recording is stopped.
 */
public class GifRecorder implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(GifRecorder.class);

    private final List<byte[]> frames;
    private final int captureIntervalMs;
    private final int frameDelayMs;
    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> captureTask;
    private boolean recording;

    /**
     * Create a GIF recorder with default settings.
     * Captures a frame every 500ms, plays back at 500ms per frame.
     */
    public GifRecorder() {
        this(500, 500);
    }

    /**
     * Create a GIF recorder with custom timing.
     *
     * @param captureIntervalMs time between screenshot captures in milliseconds
     * @param frameDelayMs      delay between frames in the output GIF
     */
    public GifRecorder(int captureIntervalMs, int frameDelayMs) {
        this.captureIntervalMs = captureIntervalMs;
        this.frameDelayMs = frameDelayMs;
        this.frames = new ArrayList<>();
        this.recording = false;
    }

    /**
     * Start recording screenshots from the browser session.
     */
    public void startRecording(BrowserSession session) {
        if (recording) {
            logger.warn("Already recording, ignoring start request");
            return;
        }
        logger.info("Starting GIF recording (capture interval={}ms)", captureIntervalMs);
        frames.clear();
        recording = true;
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "gif-recorder");
            t.setDaemon(true);
            return t;
        });
        captureTask = scheduler.scheduleAtFixedRate(() -> {
            try {
                if (session.isStarted()) {
                    byte[] screenshot = session.getCurrentPage()
                            .screenshot(new com.microsoft.playwright.Page.ScreenshotOptions().setFullPage(false));
                    synchronized (frames) {
                        frames.add(screenshot);
                    }
                }
            } catch (Exception e) {
                logger.debug("Failed to capture frame: {}", e.getMessage());
            }
        }, 0, captureIntervalMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Capture a single frame manually (useful for step-based recording).
     */
    public void captureFrame(BrowserSession session) {
        if (session.isStarted()) {
            try {
                byte[] screenshot = session.getCurrentPage()
                        .screenshot(new com.microsoft.playwright.Page.ScreenshotOptions().setFullPage(false));
                synchronized (frames) {
                    frames.add(screenshot);
                }
            } catch (Exception e) {
                logger.debug("Failed to capture frame: {}", e.getMessage());
            }
        }
    }

    /**
     * Stop recording.
     */
    public void stopRecording() {
        if (!recording) {
            return;
        }
        logger.info("Stopping GIF recording ({} frames captured)", frames.size());
        recording = false;
        if (captureTask != null) {
            captureTask.cancel(false);
        }
        if (scheduler != null) {
            scheduler.shutdown();
        }
    }

    /**
     * Save the recorded frames as an animated GIF.
     *
     * @param outputPath the path to save the GIF file
     * @return the path to the saved file
     * @throws IOException if writing fails
     */
    public Path saveGif(Path outputPath) throws IOException {
        synchronized (frames) {
            if (frames.isEmpty()) {
                throw new IOException("No frames to save");
            }
            logger.info("Saving {} frames to GIF: {}", frames.size(), outputPath);

            // Convert PNG screenshots to BufferedImages
            List<BufferedImage> images = new ArrayList<>();
            for (byte[] frame : frames) {
                BufferedImage img = ImageIO.read(new ByteArrayInputStream(frame));
                if (img != null) {
                    images.add(img);
                }
            }

            if (images.isEmpty()) {
                throw new IOException("Failed to decode any frames");
            }

            writeAnimatedGif(images, outputPath);
            logger.info("GIF saved successfully: {} ({} frames)", outputPath, images.size());
            return outputPath;
        }
    }

    /**
     * Save the GIF to a temporary file and return the path.
     */
    public Path saveGifToTemp() throws IOException {
        Path tempFile = Files.createTempFile("browser-automation-", ".gif");
        return saveGif(tempFile);
    }

    /**
     * Write an animated GIF from a list of BufferedImages.
     */
    private void writeAnimatedGif(List<BufferedImage> images, Path outputPath) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("gif");
        if (!writers.hasNext()) {
            throw new IOException("No GIF writer available");
        }
        ImageWriter writer = writers.next();

        try (ImageOutputStream ios = ImageIO.createImageOutputStream(Files.newOutputStream(outputPath))) {
            writer.setOutput(ios);
            writer.prepareWriteSequence(null);

            for (int i = 0; i < images.size(); i++) {
                BufferedImage img = images.get(i);
                ImageWriteParam params = writer.getDefaultWriteParam();
                IIOMetadata metadata = writer.getDefaultImageMetadata(
                        ImageTypeSpecifier.createFromRenderedImage(img), params);

                configureGifMetadata(metadata, i == 0);

                writer.writeToSequence(
                        new javax.imageio.IIOImage(img, null, metadata), params);
            }

            writer.endWriteSequence();
        } finally {
            writer.dispose();
        }
    }

    /**
     * Configure GIF metadata for animation.
     */
    private void configureGifMetadata(IIOMetadata metadata, boolean isFirst) throws IOException {
        String nativeFormat = metadata.getNativeMetadataFormatName();
        IIOMetadataNode root = (IIOMetadataNode) metadata.getAsTree(nativeFormat);

        // Graphics control extension - set delay
        IIOMetadataNode gce = getOrCreateNode(root, "GraphicControlExtension");
        gce.setAttribute("disposalMethod", "restoreToBackgroundColor");
        gce.setAttribute("userInputFlag", "FALSE");
        gce.setAttribute("transparentColorFlag", "FALSE");
        gce.setAttribute("delayTime", String.valueOf(frameDelayMs / 10)); // In 1/100 seconds
        gce.setAttribute("transparentColorIndex", "0");

        // Application extension - loop forever
        if (isFirst) {
            IIOMetadataNode appExtensions = getOrCreateNode(root, "ApplicationExtensions");
            IIOMetadataNode appExt = new IIOMetadataNode("ApplicationExtension");
            appExt.setAttribute("applicationID", "NETSCAPE");
            appExt.setAttribute("authenticationCode", "2.0");
            appExt.setUserObject(new byte[]{0x01, 0x00, 0x00}); // Loop forever
            appExtensions.appendChild(appExt);
        }

        metadata.setFromTree(nativeFormat, root);
    }

    private IIOMetadataNode getOrCreateNode(IIOMetadataNode root, String nodeName) {
        for (int i = 0; i < root.getLength(); i++) {
            if (root.item(i).getNodeName().equalsIgnoreCase(nodeName)) {
                return (IIOMetadataNode) root.item(i);
            }
        }
        IIOMetadataNode node = new IIOMetadataNode(nodeName);
        root.appendChild(node);
        return node;
    }

    /**
     * Get the number of captured frames.
     */
    public int getFrameCount() {
        synchronized (frames) {
            return frames.size();
        }
    }

    /**
     * Check if currently recording.
     */
    public boolean isRecording() {
        return recording;
    }

    @Override
    public void close() {
        stopRecording();
    }
}
