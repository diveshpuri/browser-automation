package com.browserautomation.filesystem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * File system abstraction for browser automation.
 *
 * Provides file operations for downloads, uploads, screenshots,
 * and temporary file management during automation sessions.
 */
public class FileSystemService {

    private static final Logger logger = LoggerFactory.getLogger(FileSystemService.class);

    private final Path baseDirectory;
    private final Path downloadsDirectory;
    private final Path screenshotsDirectory;
    private final Path tempDirectory;

    /**
     * Create a file system service with default directories.
     */
    public FileSystemService() {
        this(Path.of(System.getProperty("java.io.tmpdir"), "browser-automation"));
    }

    /**
     * Create a file system service rooted at the given directory.
     */
    public FileSystemService(Path baseDirectory) {
        this.baseDirectory = baseDirectory;
        this.downloadsDirectory = baseDirectory.resolve("downloads");
        this.screenshotsDirectory = baseDirectory.resolve("screenshots");
        this.tempDirectory = baseDirectory.resolve("temp");
        initDirectories();
    }

    private void initDirectories() {
        try {
            Files.createDirectories(baseDirectory);
            Files.createDirectories(downloadsDirectory);
            Files.createDirectories(screenshotsDirectory);
            Files.createDirectories(tempDirectory);
        } catch (IOException e) {
            logger.warn("Failed to create directories: {}", e.getMessage());
        }
    }

    /**
     * Read a file as text.
     */
    public String readFile(Path path) throws IOException {
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    /**
     * Read a file as bytes.
     */
    public byte[] readFileBytes(Path path) throws IOException {
        return Files.readAllBytes(path);
    }

    /**
     * Write text to a file.
     */
    public Path writeFile(Path path, String content) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, content, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        logger.info("Wrote file: {}", path);
        return path;
    }

    /**
     * Write bytes to a file.
     */
    public Path writeFile(Path path, byte[] content) throws IOException {
        Files.createDirectories(path.getParent());
        Files.write(path, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        logger.info("Wrote file: {} ({} bytes)", path, content.length);
        return path;
    }

    /**
     * Append text to a file.
     */
    public Path appendFile(Path path, String content) throws IOException {
        Files.writeString(path, content, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        return path;
    }

    /**
     * Copy a file from source to destination.
     */
    public Path copyFile(Path source, Path destination) throws IOException {
        Files.createDirectories(destination.getParent());
        return Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * Move a file from source to destination.
     */
    public Path moveFile(Path source, Path destination) throws IOException {
        Files.createDirectories(destination.getParent());
        return Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * Delete a file.
     */
    public boolean deleteFile(Path path) throws IOException {
        return Files.deleteIfExists(path);
    }

    /**
     * Check if a file exists.
     */
    public boolean exists(Path path) {
        return Files.exists(path);
    }

    /**
     * Get the size of a file in bytes.
     */
    public long getFileSize(Path path) throws IOException {
        return Files.size(path);
    }

    /**
     * List files in a directory.
     */
    public List<Path> listFiles(Path directory) throws IOException {
        List<Path> files = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory)) {
            for (Path entry : stream) {
                files.add(entry);
            }
        }
        return files;
    }

    /**
     * List files matching a glob pattern.
     */
    public List<Path> listFiles(Path directory, String glob) throws IOException {
        List<Path> files = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, glob)) {
            for (Path entry : stream) {
                files.add(entry);
            }
        }
        return files;
    }

    /**
     * Find files recursively matching a glob pattern.
     */
    public List<Path> findFiles(Path directory, String glob) throws IOException {
        try (Stream<Path> stream = Files.walk(directory)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().matches(globToRegex(glob)))
                    .collect(Collectors.toList());
        }
    }

    /**
     * Create a temporary file and return its path.
     */
    public Path createTempFile(String prefix, String suffix) throws IOException {
        return Files.createTempFile(tempDirectory, prefix, suffix);
    }

    /**
     * Save a screenshot to the screenshots directory.
     */
    public Path saveScreenshot(byte[] data, String name) throws IOException {
        Path path = screenshotsDirectory.resolve(name);
        return writeFile(path, data);
    }

    /**
     * Get the path for a download file.
     */
    public Path getDownloadPath(String filename) {
        return downloadsDirectory.resolve(filename);
    }

    /**
     * Clean up temporary files.
     */
    public void cleanTemp() throws IOException {
        if (Files.exists(tempDirectory)) {
            try (Stream<Path> stream = Files.walk(tempDirectory)) {
                stream.filter(Files::isRegularFile)
                        .forEach(p -> {
                            try {
                                Files.delete(p);
                            } catch (IOException e) {
                                logger.debug("Failed to delete temp file: {}", p);
                            }
                        });
            }
        }
    }

    private String globToRegex(String glob) {
        StringBuilder regex = new StringBuilder();
        for (char c : glob.toCharArray()) {
            switch (c) {
                case '*' -> regex.append(".*");
                case '?' -> regex.append(".");
                case '.' -> regex.append("\\.");
                default -> regex.append(c);
            }
        }
        return regex.toString();
    }

    // Getters
    public Path getBaseDirectory() { return baseDirectory; }
    public Path getDownloadsDirectory() { return downloadsDirectory; }
    public Path getScreenshotsDirectory() { return screenshotsDirectory; }
    public Path getTempDirectory() { return tempDirectory; }
}
