package com.browserautomation.filesystem;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for FileSystemService.
 */
class FileSystemServiceTest {

    @Test
    void testConstructorCreatesDirectories(@TempDir Path tempDir) {
        Path base = tempDir.resolve("fs-test");
        FileSystemService fs = new FileSystemService(base);
        assertTrue(Files.exists(fs.getBaseDirectory()));
        assertTrue(Files.exists(fs.getDownloadsDirectory()));
        assertTrue(Files.exists(fs.getScreenshotsDirectory()));
        assertTrue(Files.exists(fs.getTempDirectory()));
    }

    @Test
    void testWriteAndReadFile(@TempDir Path tempDir) throws IOException {
        FileSystemService fs = new FileSystemService(tempDir.resolve("fs-test"));
        Path file = fs.getBaseDirectory().resolve("test.txt");
        fs.writeFile(file, "hello world");
        assertEquals("hello world", fs.readFile(file));
    }

    @Test
    void testWriteAndReadBytes(@TempDir Path tempDir) throws IOException {
        FileSystemService fs = new FileSystemService(tempDir.resolve("fs-test"));
        Path file = fs.getBaseDirectory().resolve("test.bin");
        byte[] data = new byte[]{1, 2, 3, 4, 5};
        fs.writeFile(file, data);
        assertArrayEquals(data, fs.readFileBytes(file));
    }

    @Test
    void testAppendFile(@TempDir Path tempDir) throws IOException {
        FileSystemService fs = new FileSystemService(tempDir.resolve("fs-test"));
        Path file = fs.getBaseDirectory().resolve("append.txt");
        fs.writeFile(file, "hello");
        fs.appendFile(file, " world");
        assertEquals("hello world", fs.readFile(file));
    }

    @Test
    void testCopyFile(@TempDir Path tempDir) throws IOException {
        FileSystemService fs = new FileSystemService(tempDir.resolve("fs-test"));
        Path src = fs.getBaseDirectory().resolve("src.txt");
        Path dst = fs.getBaseDirectory().resolve("dst.txt");
        fs.writeFile(src, "copy me");
        fs.copyFile(src, dst);
        assertEquals("copy me", fs.readFile(dst));
        assertTrue(fs.exists(src));
    }

    @Test
    void testMoveFile(@TempDir Path tempDir) throws IOException {
        FileSystemService fs = new FileSystemService(tempDir.resolve("fs-test"));
        Path src = fs.getBaseDirectory().resolve("src.txt");
        Path dst = fs.getBaseDirectory().resolve("dst.txt");
        fs.writeFile(src, "move me");
        fs.moveFile(src, dst);
        assertEquals("move me", fs.readFile(dst));
        assertFalse(fs.exists(src));
    }

    @Test
    void testDeleteFile(@TempDir Path tempDir) throws IOException {
        FileSystemService fs = new FileSystemService(tempDir.resolve("fs-test"));
        Path file = fs.getBaseDirectory().resolve("del.txt");
        fs.writeFile(file, "delete");
        assertTrue(fs.deleteFile(file));
        assertFalse(fs.exists(file));
    }

    @Test
    void testGetFileSize(@TempDir Path tempDir) throws IOException {
        FileSystemService fs = new FileSystemService(tempDir.resolve("fs-test"));
        Path file = fs.getBaseDirectory().resolve("size.txt");
        fs.writeFile(file, "12345");
        assertEquals(5, fs.getFileSize(file));
    }

    @Test
    void testListFiles(@TempDir Path tempDir) throws IOException {
        FileSystemService fs = new FileSystemService(tempDir.resolve("fs-test"));
        fs.writeFile(fs.getBaseDirectory().resolve("a.txt"), "a");
        fs.writeFile(fs.getBaseDirectory().resolve("b.txt"), "b");
        List<Path> files = fs.listFiles(fs.getBaseDirectory(), "*.txt");
        assertEquals(2, files.size());
    }

    @Test
    void testCreateTempFile(@TempDir Path tempDir) throws IOException {
        FileSystemService fs = new FileSystemService(tempDir.resolve("fs-test"));
        Path temp = fs.createTempFile("test-", ".tmp");
        assertTrue(Files.exists(temp));
        assertTrue(temp.getFileName().toString().startsWith("test-"));
    }

    @Test
    void testSaveScreenshot(@TempDir Path tempDir) throws IOException {
        FileSystemService fs = new FileSystemService(tempDir.resolve("fs-test"));
        byte[] data = new byte[]{10, 20, 30};
        Path path = fs.saveScreenshot(data, "test.png");
        assertTrue(Files.exists(path));
        assertArrayEquals(data, Files.readAllBytes(path));
    }

    @Test
    void testGetDownloadPath(@TempDir Path tempDir) {
        FileSystemService fs = new FileSystemService(tempDir.resolve("fs-test"));
        Path path = fs.getDownloadPath("file.zip");
        assertTrue(path.toString().contains("downloads"));
        assertTrue(path.toString().endsWith("file.zip"));
    }

    @Test
    void testCleanTemp(@TempDir Path tempDir) throws IOException {
        FileSystemService fs = new FileSystemService(tempDir.resolve("fs-test"));
        Path temp = fs.createTempFile("clean-", ".tmp");
        Files.writeString(temp, "temp data");
        fs.cleanTemp();
        assertFalse(Files.exists(temp));
    }
}
