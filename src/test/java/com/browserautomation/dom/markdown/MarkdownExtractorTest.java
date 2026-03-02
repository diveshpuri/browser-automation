package com.browserautomation.dom.markdown;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MarkdownExtractorTest {

    private MarkdownExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new MarkdownExtractor();
    }

    @Test
    void testSplitIntoChunksWithHeaders() {
        String markdown = """
                # Title
                Some content here.
                
                ## Section 1
                Content for section 1.
                More content.
                
                ## Section 2
                Content for section 2.
                """;

        List<MarkdownExtractor.MarkdownChunk> chunks = extractor.splitIntoChunks(markdown);
        assertFalse(chunks.isEmpty());
        assertTrue(chunks.size() >= 2);
    }

    @Test
    void testSplitIntoChunksEmpty() {
        // Empty string splits to [""] which produces 1 chunk with empty content
        List<MarkdownExtractor.MarkdownChunk> chunks = extractor.splitIntoChunks("");
        assertEquals(1, chunks.size());
        assertTrue(chunks.get(0).content().isEmpty());
    }

    @Test
    void testSplitIntoChunksNoHeaders() {
        String markdown = "Just plain text without any headers.\nMore text here.";
        List<MarkdownExtractor.MarkdownChunk> chunks = extractor.splitIntoChunks(markdown);
        assertEquals(1, chunks.size());
    }

    @Test
    void testChunkSizeLimit() {
        MarkdownExtractor smallChunkExtractor = new MarkdownExtractor(
                new MarkdownExtractor.MarkdownConfig().maxChunkSize(50));

        StringBuilder longContent = new StringBuilder("# Header\n");
        for (int i = 0; i < 20; i++) {
            longContent.append("Line ").append(i).append(" with some content.\n");
        }

        List<MarkdownExtractor.MarkdownChunk> chunks = smallChunkExtractor.splitIntoChunks(longContent.toString());
        assertTrue(chunks.size() > 1);
    }

    @Test
    void testMarkdownChunkRecord() {
        var chunk = new MarkdownExtractor.MarkdownChunk("# Header", "Content text", 0, 5);
        assertEquals("# Header", chunk.header());
        assertEquals("Content text", chunk.content());
        assertEquals(0, chunk.startLine());
        assertEquals(5, chunk.endLine());
        assertEquals(12, chunk.length());
    }

    @Test
    void testMarkdownConfig() {
        var config = new MarkdownExtractor.MarkdownConfig()
                .maxChunkSize(8000)
                .overlapLines(5)
                .removeJsonBlobs(false);

        assertEquals(8000, config.getMaxChunkSize());
        assertEquals(5, config.getOverlapLines());
        assertFalse(config.isRemoveJsonBlobs());
    }

    @Test
    void testDefaultConfig() {
        var config = new MarkdownExtractor.MarkdownConfig();
        assertEquals(4000, config.getMaxChunkSize());
        assertEquals(3, config.getOverlapLines());
        assertTrue(config.isRemoveJsonBlobs());
    }

    @Test
    void testOverlapBetweenChunks() {
        MarkdownExtractor overlapExtractor = new MarkdownExtractor(
                new MarkdownExtractor.MarkdownConfig().overlapLines(2));

        String markdown = """
                # Section 1
                Line 1 of section 1
                Line 2 of section 1
                Line 3 of section 1
                
                # Section 2
                Line 1 of section 2
                """;

        List<MarkdownExtractor.MarkdownChunk> chunks = overlapExtractor.splitIntoChunks(markdown);
        assertTrue(chunks.size() >= 2);
    }
}
