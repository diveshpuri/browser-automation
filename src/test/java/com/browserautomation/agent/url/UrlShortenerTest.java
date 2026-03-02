package com.browserautomation.agent.url;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UrlShortenerTest {

    private UrlShortener shortener;

    @BeforeEach
    void setUp() {
        shortener = new UrlShortener(50);
    }

    @Test
    void testShortenLongUrl() {
        String longUrl = "https://www.example.com/very/long/path/to/some/resource?param=value&other=thing";
        String shortened = shortener.shorten(longUrl);

        assertNotEquals(longUrl, shortened);
        assertTrue(shortened.startsWith("[url:"));
        assertTrue(shortened.endsWith("]"));
        assertTrue(shortened.length() < longUrl.length());
    }

    @Test
    void testShortUrlUnchanged() {
        String shortUrl = "https://example.com/page";
        String result = shortener.shorten(shortUrl);
        assertEquals(shortUrl, result);
    }

    @Test
    void testShortenUrlsInText() {
        String text = "Visit https://www.example.com/very/long/path/to/some/resource?param=value&other=thing for details";
        String result = shortener.shortenUrls(text);

        assertFalse(result.contains("very/long/path"));
        assertTrue(result.contains("[url:"));
        assertTrue(result.contains("for details"));
    }

    @Test
    void testRestoreUrls() {
        String longUrl = "https://www.example.com/very/long/path/to/some/resource?param=value&other=thing";
        String text = "Visit " + longUrl + " for details";
        String shortened = shortener.shortenUrls(text);
        String restored = shortener.restoreUrls(shortened);

        assertEquals(text, restored);
    }

    @Test
    void testGetOriginal() {
        String longUrl = "https://www.example.com/very/long/path/to/some/resource?param=value";
        String shortened = shortener.shorten(longUrl);

        assertEquals(longUrl, shortener.getOriginal(shortened));
    }

    @Test
    void testSameUrlSameShortened() {
        String longUrl = "https://www.example.com/very/long/path/to/some/resource?param=value";
        String first = shortener.shorten(longUrl);
        String second = shortener.shorten(longUrl);

        assertEquals(first, second);
    }

    @Test
    void testMultipleUrls() {
        String url1 = "https://www.example.com/very/long/path/to/resource/one?param=value";
        String url2 = "https://www.other.com/another/very/long/path/to/resource?q=search";

        shortener.shorten(url1);
        shortener.shorten(url2);

        assertEquals(2, shortener.getShortenedCount());
    }

    @Test
    void testGetCharactersSaved() {
        String longUrl = "https://www.example.com/very/long/path/to/some/resource?param=value&other=thing";
        String shortened = shortener.shorten(longUrl);

        int saved = shortener.getCharactersSaved();
        assertTrue(saved > 0);
        assertEquals(longUrl.length() - shortened.length(), saved);
    }

    @Test
    void testClear() {
        shortener.shorten("https://www.example.com/very/long/path/to/some/resource?param=value");
        assertEquals(1, shortener.getShortenedCount());

        shortener.clear();
        assertEquals(0, shortener.getShortenedCount());
    }

    @Test
    void testNullInput() {
        assertNull(shortener.shortenUrls(null));
        assertNull(shortener.restoreUrls(null));
        assertNull(shortener.shorten(null));
    }

    @Test
    void testEmptyInput() {
        assertEquals("", shortener.shortenUrls(""));
        assertEquals("", shortener.restoreUrls(""));
    }

    @Test
    void testGetMappings() {
        String longUrl = "https://www.example.com/very/long/path/to/some/resource?param=value";
        shortener.shorten(longUrl);

        assertFalse(shortener.getMappings().isEmpty());
        assertTrue(shortener.getMappings().containsValue(longUrl));
    }
}
