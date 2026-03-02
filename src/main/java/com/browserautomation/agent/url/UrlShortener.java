package com.browserautomation.agent.url;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Auto-shortens long URLs in LLM messages to reduce token usage.
 * Replaces long URLs with shortened placeholders and maintains a mapping
 * for restoration when needed.
 *
 */
public class UrlShortener {

    private static final Logger logger = LoggerFactory.getLogger(UrlShortener.class);

    private static final Pattern URL_PATTERN = Pattern.compile(
            "(https?://[^\\s\"'<>\\]\\)]+)", Pattern.CASE_INSENSITIVE);

    private final int maxUrlLength;
    private final Map<String, String> shortenedToOriginal;
    private final Map<String, String> originalToShortened;
    private int counter;

    public UrlShortener() {
        this(80);
    }

    public UrlShortener(int maxUrlLength) {
        this.maxUrlLength = maxUrlLength;
        this.shortenedToOriginal = new LinkedHashMap<>();
        this.originalToShortened = new LinkedHashMap<>();
        this.counter = 0;
    }

    /**
     * Shorten all long URLs in the given text.
     *
     * @param text the text containing URLs
     * @return the text with long URLs shortened
     */
    public String shortenUrls(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        Matcher matcher = URL_PATTERN.matcher(text);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String url = matcher.group(1);
            if (url.length() > maxUrlLength) {
                String shortened = getOrCreateShortened(url);
                matcher.appendReplacement(result, Matcher.quoteReplacement(shortened));
            }
        }
        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * Restore all shortened URLs back to their originals.
     *
     * @param text the text with shortened URLs
     * @return the text with original URLs restored
     */
    public String restoreUrls(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        String result = text;
        for (Map.Entry<String, String> entry : shortenedToOriginal.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }
        return result;
    }

    /**
     * Shorten a single URL.
     *
     * @param url the URL to shorten
     * @return the shortened form, or the original if it's short enough
     */
    public String shorten(String url) {
        if (url == null || url.length() <= maxUrlLength) {
            return url;
        }
        return getOrCreateShortened(url);
    }

    /**
     * Get the original URL for a shortened form.
     *
     * @param shortened the shortened URL
     * @return the original URL, or null if not found
     */
    public String getOriginal(String shortened) {
        return shortenedToOriginal.get(shortened);
    }

    /**
     * Get all URL mappings.
     *
     * @return unmodifiable map of shortened -> original URLs
     */
    public Map<String, String> getMappings() {
        return Collections.unmodifiableMap(shortenedToOriginal);
    }

    /**
     * Get the number of shortened URLs.
     */
    public int getShortenedCount() {
        return shortenedToOriginal.size();
    }

    /**
     * Get the total characters saved by shortening.
     */
    public int getCharactersSaved() {
        int saved = 0;
        for (Map.Entry<String, String> entry : shortenedToOriginal.entrySet()) {
            saved += entry.getValue().length() - entry.getKey().length();
        }
        return saved;
    }

    /**
     * Clear all mappings.
     */
    public void clear() {
        shortenedToOriginal.clear();
        originalToShortened.clear();
        counter = 0;
    }

    private String getOrCreateShortened(String url) {
        String existing = originalToShortened.get(url);
        if (existing != null) {
            return existing;
        }

        String shortened = createShortForm(url);
        shortenedToOriginal.put(shortened, url);
        originalToShortened.put(url, shortened);
        return shortened;
    }

    private String createShortForm(String url) {
        counter++;
        try {
            URI uri = URI.create(url);
            String host = uri.getHost();
            String path = uri.getPath();
            if (host != null) {
                // Create a meaningful short form: host + truncated path + ID
                String shortPath = "";
                if (path != null && path.length() > 1) {
                    // Get first path segment
                    String[] segments = path.split("/");
                    if (segments.length > 1) {
                        shortPath = "/" + segments[1];
                        if (segments.length > 2) {
                            shortPath += "/...";
                        }
                    }
                }
                return "[url:" + host + shortPath + "#" + counter + "]";
            }
        } catch (Exception e) {
            // Fall through to generic shortening
        }
        return "[url:#" + counter + "]";
    }
}
