package com.browserautomation.dom;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ShadowDomService.
 * Tests the Shadow DOM service configuration and selector integration
 * without requiring a live browser.
 */
class ShadowDomServiceTest {

    @Test
    void testDefaultConstructorEnablesShadowDom() {
        ShadowDomService service = new ShadowDomService();
        assertTrue(service.isShadowDomEnabled());
    }

    @Test
    void testConstructorWithDisabledShadowDom() {
        ShadowDomService service = new ShadowDomService(false);
        assertFalse(service.isShadowDomEnabled());
    }

    @Test
    void testSetShadowDomEnabled() {
        ShadowDomService service = new ShadowDomService();
        service.setShadowDomEnabled(false);
        assertFalse(service.isShadowDomEnabled());
        service.setShadowDomEnabled(true);
        assertTrue(service.isShadowDomEnabled());
    }

    @Test
    void testGetSelectorScorer() {
        ShadowDomService service = new ShadowDomService();
        assertNotNull(service.getSelectorScorer());
    }

    @Test
    void testGetBestSelectorForNormalElement() {
        ShadowDomService service = new ShadowDomService();
        Map<String, String> attrs = new HashMap<>();
        attrs.put("data-testid", "submit-btn");
        DomElement element = new DomElement(0, "button", attrs, "Submit",
                true, true, false, "button", "", null);

        String selector = service.getBestSelector(element);
        assertNotNull(selector);
        assertTrue(selector.contains("data-testid"));
    }

    @Test
    void testGetBestSelectorForShadowDomElement() {
        ShadowDomService service = new ShadowDomService();
        Map<String, String> attrs = new HashMap<>();
        attrs.put("data-testid", "inner-input");
        attrs.put("data-shadow-host", "my-component");
        attrs.put("data-in-shadow-dom", "true");
        DomElement element = new DomElement(0, "input", attrs, "",
                true, true, false, "textbox", "", null);

        String selector = service.getBestSelector(element);
        assertNotNull(selector);
        // Shadow DOM elements should use >> piercing combinator
        assertTrue(selector.contains(">>"));
        assertTrue(selector.contains("my-component"));
    }

    @Test
    void testGetScoredSelectorsForElement() {
        ShadowDomService service = new ShadowDomService();
        Map<String, String> attrs = new HashMap<>();
        attrs.put("id", "search-box");
        attrs.put("name", "q");
        attrs.put("placeholder", "Search...");
        DomElement element = new DomElement(0, "input", attrs, "",
                true, true, false, "searchbox", "", null);

        List<SelectorStrategy> selectors = service.getScoredSelectors(element);
        assertFalse(selectors.isEmpty());
        // Should have multiple strategies
        assertTrue(selectors.size() >= 3);
        // Should be sorted by score descending
        for (int i = 0; i < selectors.size() - 1; i++) {
            assertTrue(selectors.get(i).getScore() >= selectors.get(i + 1).getScore());
        }
    }

    @Test
    void testGetBestSelectorFallsBackToBasicSelector() {
        ShadowDomService service = new ShadowDomService();
        Map<String, String> attrs = new HashMap<>();
        DomElement element = new DomElement(0, "div", attrs, "",
                true, false, false, "", "", null);

        String selector = service.getBestSelector(element);
        assertNotNull(selector);
        // Should at least return the tag name
        assertEquals("div", selector);
    }

    @Test
    void testShadowDomElementAttributes() {
        Map<String, String> attrs = new HashMap<>();
        attrs.put("data-shadow-host", "app-header");
        attrs.put("data-in-shadow-dom", "true");
        attrs.put("data-testid", "nav-link");
        DomElement element = new DomElement(0, "a", attrs, "Home",
                true, true, false, "link", "", null);

        assertTrue(element.isInShadowDom());
        assertEquals("app-header", element.getShadowHostSelector());
    }

    @Test
    void testNonShadowDomElementAttributes() {
        Map<String, String> attrs = Map.of("id", "my-link");
        DomElement element = new DomElement(0, "a", attrs, "Home",
                true, true, false, "link", "", null);

        assertFalse(element.isInShadowDom());
        assertNull(element.getShadowHostSelector());
    }

    @Test
    void testBuildScoredSelector() {
        Map<String, String> attrs = new HashMap<>();
        attrs.put("data-testid", "main-button");
        DomElement element = new DomElement(0, "button", attrs, "Click",
                true, true, false, "button", "", null);

        String scored = element.buildScoredSelector();
        assertNotNull(scored);
        assertTrue(scored.contains("data-testid"));
    }

    @Test
    void testBuildScoredSelectorWithShadowDom() {
        Map<String, String> attrs = new HashMap<>();
        attrs.put("id", "inner-btn");
        DomElement element = new DomElement(0, "button", attrs, "OK",
                true, true, false, "button", "", null);

        String scored = element.buildScoredSelector(true);
        assertNotNull(scored);
    }

    @Test
    void testGetScoredSelectorsFromElement() {
        Map<String, String> attrs = new HashMap<>();
        attrs.put("id", "my-form");
        attrs.put("name", "login");
        attrs.put("aria-label", "Login form");
        DomElement element = new DomElement(0, "form", attrs, "",
                true, true, false, "form", "Login form", null);

        List<SelectorStrategy> selectors = element.getScoredSelectors();
        assertFalse(selectors.isEmpty());
        assertTrue(selectors.size() >= 2);
    }
}
