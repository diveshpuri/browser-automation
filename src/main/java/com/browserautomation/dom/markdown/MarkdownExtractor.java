package com.browserautomation.dom.markdown;

import com.microsoft.playwright.Page;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Structure-aware markdown extraction from web pages.
 * Handles header-preferred splitting, JSON blob removal for SPAs,
 * and overlap prefixes for context carry.
 *
 */
public class MarkdownExtractor {

    private static final Logger logger = LoggerFactory.getLogger(MarkdownExtractor.class);

    private static final Pattern JSON_BLOB_PATTERN = Pattern.compile(
            "\\{[\\s\\S]{500,}?\\}", Pattern.MULTILINE);
    private static final Pattern SCRIPT_PATTERN = Pattern.compile(
            "<script[^>]*>[\\s\\S]*?</script>", Pattern.CASE_INSENSITIVE);
    private static final Pattern STYLE_PATTERN = Pattern.compile(
            "<style[^>]*>[\\s\\S]*?</style>", Pattern.CASE_INSENSITIVE);
    private static final Pattern MULTIPLE_NEWLINES = Pattern.compile("\\n{3,}");
    private static final Pattern MULTIPLE_SPACES = Pattern.compile(" {2,}");

    private final MarkdownConfig config;

    public MarkdownExtractor() {
        this(new MarkdownConfig());
    }

    public MarkdownExtractor(MarkdownConfig config) {
        this.config = config;
    }

    /**
     * Extract page content as markdown text.
     */
    @SuppressWarnings("unchecked")
    public String extractMarkdown(Page page) {
        try {
            // Extract structured content via JS
            Object result = page.evaluate(EXTRACT_CONTENT_JS);
            if (result instanceof Map) {
                Map<String, Object> content = (Map<String, Object>) result;
                String title = (String) content.getOrDefault("title", "");
                String bodyText = (String) content.getOrDefault("bodyText", "");
                Object headingsObj = content.get("headings");

                StringBuilder markdown = new StringBuilder();

                // Title
                if (!title.isEmpty()) {
                    markdown.append("# ").append(title).append("\n\n");
                }

                // Process body text
                String processed = processContent(bodyText);

                // Remove JSON blobs (common in SPAs like LinkedIn, Facebook)
                if (config.isRemoveJsonBlobs()) {
                    processed = JSON_BLOB_PATTERN.matcher(processed).replaceAll("[JSON data removed]");
                }

                markdown.append(processed);
                return markdown.toString().trim();
            }
        } catch (Exception e) {
            logger.warn("Markdown extraction failed: {}", e.getMessage());
        }
        return "";
    }

    /**
     * Extract markdown and split into chunks for processing.
     */
    public List<MarkdownChunk> extractChunks(Page page) {
        String markdown = extractMarkdown(page);
        return splitIntoChunks(markdown);
    }

    /**
     * Split markdown into header-preferred chunks with overlap.
     */
    public List<MarkdownChunk> splitIntoChunks(String markdown) {
        List<MarkdownChunk> chunks = new ArrayList<>();
        String[] lines = markdown.split("\n");
        StringBuilder currentChunk = new StringBuilder();
        String currentHeader = "";
        int currentChunkStart = 0;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];

            // Check if this is a header line
            if (line.startsWith("#")) {
                // Save current chunk if it has content
                if (!currentChunk.isEmpty()) {
                    chunks.add(new MarkdownChunk(currentHeader, currentChunk.toString().trim(),
                            currentChunkStart, i - 1));
                    currentChunk = new StringBuilder();

                    // Add overlap prefix from previous chunk for context carry
                    if (config.getOverlapLines() > 0 && i > 0) {
                        int overlapStart = Math.max(0, i - config.getOverlapLines());
                        for (int j = overlapStart; j < i; j++) {
                            currentChunk.append(lines[j]).append("\n");
                        }
                    }
                }
                currentHeader = line;
                currentChunkStart = i;
            }

            currentChunk.append(line).append("\n");

            // Check chunk size limit
            if (currentChunk.length() > config.getMaxChunkSize()) {
                chunks.add(new MarkdownChunk(currentHeader, currentChunk.toString().trim(),
                        currentChunkStart, i));
                currentChunk = new StringBuilder();
                currentHeader = "";
                currentChunkStart = i + 1;
            }
        }

        // Add remaining chunk
        if (!currentChunk.isEmpty()) {
            chunks.add(new MarkdownChunk(currentHeader, currentChunk.toString().trim(),
                    currentChunkStart, lines.length - 1));
        }

        return chunks;
    }

    private String processContent(String content) {
        // Remove script and style tags
        content = SCRIPT_PATTERN.matcher(content).replaceAll("");
        content = STYLE_PATTERN.matcher(content).replaceAll("");

        // Normalize whitespace
        content = MULTIPLE_NEWLINES.matcher(content).replaceAll("\n\n");
        content = MULTIPLE_SPACES.matcher(content).replaceAll(" ");

        // Trim lines
        String[] lines = content.split("\n");
        StringBuilder result = new StringBuilder();
        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty() || result.length() == 0 || result.charAt(result.length() - 1) != '\n') {
                result.append(trimmed).append("\n");
            }
        }

        return result.toString();
    }

    /**
     * JavaScript for extracting structured content from the page.
     */
    private static final String EXTRACT_CONTENT_JS = """
        () => {
            const title = document.title || '';
            const bodyText = document.body ? document.body.innerText : '';
            const headings = [];
            document.querySelectorAll('h1, h2, h3, h4, h5, h6').forEach(h => {
                headings.push({
                    level: parseInt(h.tagName.substring(1)),
                    text: h.innerText.trim()
                });
            });
            return { title, bodyText, headings };
        }
    """;

    /**
     * A chunk of extracted markdown content.
     */
    public record MarkdownChunk(String header, String content, int startLine, int endLine) {
        public int length() { return content.length(); }
    }

    /**
     * Configuration for markdown extraction.
     */
    public static class MarkdownConfig {
        private int maxChunkSize = 4000;
        private int overlapLines = 3;
        private boolean removeJsonBlobs = true;

        public MarkdownConfig maxChunkSize(int v) { maxChunkSize = v; return this; }
        public MarkdownConfig overlapLines(int v) { overlapLines = v; return this; }
        public MarkdownConfig removeJsonBlobs(boolean v) { removeJsonBlobs = v; return this; }

        public int getMaxChunkSize() { return maxChunkSize; }
        public int getOverlapLines() { return overlapLines; }
        public boolean isRemoveJsonBlobs() { return removeJsonBlobs; }
    }
}
