package com.browserautomation.dom;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for SelectorScorer - the scoring mechanism for element selectors.
 */
class SelectorScorerTest {

    private SelectorScorer scorer;

    @BeforeEach
    void setUp() {
        scorer = new SelectorScorer();
    }

    // --- Helper to create DomElements ---

    private DomElement createElement(String tag, Map<String, String> attrs) {
        return createElement(tag, attrs, "", "");
    }

    private DomElement createElement(String tag, Map<String, String> attrs, String text, String role) {
        return new DomElement(0, tag, attrs, text, true, true, false, role, attrs.getOrDefault("aria-label", ""), null);
    }

    // --- data-testid strategy tests ---

    @Test
    void testDataTestIdGetsHighestScore() {
        Map<String, String> attrs = new HashMap<>();
        attrs.put("data-testid", "submit-button");
        attrs.put("id", "btn1");
        attrs.put("class", "primary-btn");

        DomElement element = createElement("button", attrs, "Submit", "button");
        List<SelectorStrategy> candidates = scorer.generateAndScoreCandidates(element);

        assertFalse(candidates.isEmpty());
        // data-testid should be ranked first (highest score)
        assertEquals(SelectorStrategy.Strategy.DATA_TEST_ID, candidates.get(0).getStrategy());
        assertTrue(candidates.get(0).getScore() >= 90.0);
    }

    @Test
    void testDataCyAttributeRecognized() {
        Map<String, String> attrs = Map.of("data-cy", "login-btn");
        DomElement element = createElement("button", attrs);

        List<SelectorStrategy> candidates = scorer.generateAndScoreCandidates(element);
        boolean hasDataTestId = candidates.stream()
                .anyMatch(c -> c.getStrategy() == SelectorStrategy.Strategy.DATA_TEST_ID);
        assertTrue(hasDataTestId);
    }

    @Test
    void testDataTestAttributeRecognized() {
        Map<String, String> attrs = Map.of("data-test", "my-element");
        DomElement element = createElement("button", attrs);

        List<SelectorStrategy> candidates = scorer.generateAndScoreCandidates(element);
        boolean hasDataTestId = candidates.stream()
                .anyMatch(c -> c.getStrategy() == SelectorStrategy.Strategy.DATA_TEST_ID);
        assertTrue(hasDataTestId);
    }

    // --- ID strategy tests ---

    @Test
    void testIdSelectorScoresHigh() {
        Map<String, String> attrs = Map.of("id", "main-form");
        DomElement element = createElement("form", attrs);

        List<SelectorStrategy> candidates = scorer.generateAndScoreCandidates(element);
        boolean hasId = candidates.stream()
                .anyMatch(c -> c.getStrategy() == SelectorStrategy.Strategy.ID);
        assertTrue(hasId);

        SelectorStrategy idStrategy = candidates.stream()
                .filter(c -> c.getStrategy() == SelectorStrategy.Strategy.ID)
                .findFirst().orElseThrow();
        assertTrue(idStrategy.getScore() > 50.0);
        assertEquals("#main-form", idStrategy.getSelector());
    }

    @Test
    void testGeneratedIdFilteredOut() {
        // Angular CDK generated ID
        assertTrue(SelectorScorer.isGeneratedId("cdk-overlay-0"));
        assertTrue(SelectorScorer.isGeneratedId("mat-input-123"));
        assertTrue(SelectorScorer.isGeneratedId("_ngcontent-abc123def456"));
        // UUID-like
        assertTrue(SelectorScorer.isGeneratedId("a1b2c3d4-e5f6-7890-abcd-ef1234567890"));
        // Numeric only
        assertTrue(SelectorScorer.isGeneratedId("12345"));
        // React-style
        assertTrue(SelectorScorer.isGeneratedId(":r0:"));
        // Very long
        assertTrue(SelectorScorer.isGeneratedId("a".repeat(50)));
    }

    @Test
    void testStableIdNotFilteredOut() {
        assertFalse(SelectorScorer.isGeneratedId("login-form"));
        assertFalse(SelectorScorer.isGeneratedId("main-content"));
        assertFalse(SelectorScorer.isGeneratedId("nav-bar"));
    }

    // --- ARIA label strategy tests ---

    @Test
    void testAriaLabelStrategy() {
        Map<String, String> attrs = Map.of("aria-label", "Close dialog");
        DomElement element = createElement("button", attrs, "", "button");

        List<SelectorStrategy> candidates = scorer.generateAndScoreCandidates(element);
        boolean hasAria = candidates.stream()
                .anyMatch(c -> c.getStrategy() == SelectorStrategy.Strategy.ARIA_LABEL);
        assertTrue(hasAria);

        SelectorStrategy ariaStrategy = candidates.stream()
                .filter(c -> c.getStrategy() == SelectorStrategy.Strategy.ARIA_LABEL)
                .findFirst().orElseThrow();
        assertTrue(ariaStrategy.getSelector().contains("aria-label"));
        assertTrue(ariaStrategy.getSelector().contains("Close dialog"));
    }

    // --- Name attribute strategy tests ---

    @Test
    void testNameAttributeOnInput() {
        Map<String, String> attrs = Map.of("name", "email");
        DomElement element = createElement("input", attrs);

        List<SelectorStrategy> candidates = scorer.generateAndScoreCandidates(element);
        boolean hasName = candidates.stream()
                .anyMatch(c -> c.getStrategy() == SelectorStrategy.Strategy.NAME);
        assertTrue(hasName);

        SelectorStrategy nameStrategy = candidates.stream()
                .filter(c -> c.getStrategy() == SelectorStrategy.Strategy.NAME)
                .findFirst().orElseThrow();
        assertTrue(nameStrategy.getSelector().contains("name="));
    }

    // --- Placeholder strategy tests ---

    @Test
    void testPlaceholderStrategy() {
        Map<String, String> attrs = Map.of("placeholder", "Enter your email");
        DomElement element = createElement("input", attrs);

        List<SelectorStrategy> candidates = scorer.generateAndScoreCandidates(element);
        boolean hasPlaceholder = candidates.stream()
                .anyMatch(c -> c.getStrategy() == SelectorStrategy.Strategy.PLACEHOLDER);
        assertTrue(hasPlaceholder);
    }

    // --- CSS class strategy tests ---

    @Test
    void testStableClassesUsed() {
        Map<String, String> attrs = Map.of("class", "btn btn-primary submit-button");
        DomElement element = createElement("button", attrs);

        List<SelectorStrategy> candidates = scorer.generateAndScoreCandidates(element);
        boolean hasCssClass = candidates.stream()
                .anyMatch(c -> c.getStrategy() == SelectorStrategy.Strategy.CSS_CLASS);
        assertTrue(hasCssClass);
    }

    @Test
    void testUnstableClassesFiltered() {
        // All Angular/CSS-in-JS generated classes
        List<String> unstable = SelectorScorer.filterStableClasses("ng-star-inserted cdk-focused css-1a2b3c");
        assertTrue(unstable.isEmpty());
    }

    @Test
    void testMixedClassesFilterCorrectly() {
        List<String> stable = SelectorScorer.filterStableClasses("ng-star-inserted submit-button btn-primary css-abc123");
        assertTrue(stable.contains("submit-button"));
        assertTrue(stable.contains("btn-primary"));
        assertFalse(stable.contains("ng-star-inserted"));
        assertFalse(stable.contains("css-abc123"));
    }

    @Test
    void testStableClassDetection() {
        assertTrue(SelectorScorer.isStableClass("btn-primary"));
        assertTrue(SelectorScorer.isStableClass("header-nav"));
        assertTrue(SelectorScorer.isStableClass("card-container"));
        assertFalse(SelectorScorer.isStableClass("ng-star-inserted"));
        assertFalse(SelectorScorer.isStableClass("css-1a2b3c"));
        assertFalse(SelectorScorer.isStableClass("MuiGrid-root"));
    }

    @Test
    void testEmptyClassReturnsEmpty() {
        List<String> result = SelectorScorer.filterStableClasses("");
        assertTrue(result.isEmpty());
    }

    @Test
    void testNullClassReturnsEmpty() {
        List<String> result = SelectorScorer.filterStableClasses(null);
        assertTrue(result.isEmpty());
    }

    // --- Tag + type strategy tests ---

    @Test
    void testTagTypeStrategy() {
        Map<String, String> attrs = Map.of("type", "email");
        DomElement element = createElement("input", attrs);

        List<SelectorStrategy> candidates = scorer.generateAndScoreCandidates(element);
        boolean hasTagType = candidates.stream()
                .anyMatch(c -> c.getStrategy() == SelectorStrategy.Strategy.TAG_TYPE);
        assertTrue(hasTagType);
    }

    // --- Text content strategy tests ---

    @Test
    void testTextContentStrategy() {
        DomElement element = createElement("button", Map.of(), "Sign In", "button");

        List<SelectorStrategy> candidates = scorer.generateAndScoreCandidates(element);
        boolean hasText = candidates.stream()
                .anyMatch(c -> c.getStrategy() == SelectorStrategy.Strategy.TEXT_CONTENT);
        assertTrue(hasText);
    }

    @Test
    void testTextContentTooLongExcluded() {
        String longText = "A".repeat(100);
        DomElement element = createElement("p", Map.of(), longText, "");

        List<SelectorStrategy> candidates = scorer.generateAndScoreCandidates(element);
        boolean hasText = candidates.stream()
                .anyMatch(c -> c.getStrategy() == SelectorStrategy.Strategy.TEXT_CONTENT);
        assertFalse(hasText);
    }

    @Test
    void testTextContentTooShortExcluded() {
        DomElement element = createElement("span", Map.of(), "x", "");

        List<SelectorStrategy> candidates = scorer.generateAndScoreCandidates(element);
        boolean hasText = candidates.stream()
                .anyMatch(c -> c.getStrategy() == SelectorStrategy.Strategy.TEXT_CONTENT);
        assertFalse(hasText);
    }

    // --- Custom data attribute strategy tests ---

    @Test
    void testCustomDataAttribute() {
        Map<String, String> attrs = Map.of("data-action", "submit-form");
        DomElement element = createElement("button", attrs);

        List<SelectorStrategy> candidates = scorer.generateAndScoreCandidates(element);
        boolean hasDataAttr = candidates.stream()
                .anyMatch(c -> c.getStrategy() == SelectorStrategy.Strategy.DATA_ATTRIBUTE);
        assertTrue(hasDataAttr);
    }

    // --- Tag-only fallback tests ---

    @Test
    void testTagOnlyAlwaysPresent() {
        DomElement element = createElement("div", Map.of());

        List<SelectorStrategy> candidates = scorer.generateAndScoreCandidates(element);
        boolean hasTagOnly = candidates.stream()
                .anyMatch(c -> c.getStrategy() == SelectorStrategy.Strategy.TAG_ONLY);
        assertTrue(hasTagOnly);
    }

    @Test
    void testTagOnlyHasLowestScore() {
        Map<String, String> attrs = Map.of("id", "my-element", "data-testid", "elem");
        DomElement element = createElement("button", attrs);

        List<SelectorStrategy> candidates = scorer.generateAndScoreCandidates(element);
        SelectorStrategy tagOnly = candidates.stream()
                .filter(c -> c.getStrategy() == SelectorStrategy.Strategy.TAG_ONLY)
                .findFirst().orElseThrow();

        // Tag-only should always be the last (lowest score)
        assertEquals(candidates.get(candidates.size() - 1).getStrategy(), SelectorStrategy.Strategy.TAG_ONLY);
        assertTrue(tagOnly.getScore() < 50.0);
    }

    // --- getBestSelector tests ---

    @Test
    void testGetBestSelector() {
        Map<String, String> attrs = Map.of("data-testid", "main-btn", "id", "btn1");
        DomElement element = createElement("button", attrs, "Click me", "button");

        SelectorStrategy best = scorer.getBestSelector(element);
        assertNotNull(best);
        assertEquals(SelectorStrategy.Strategy.DATA_TEST_ID, best.getStrategy());
    }

    @Test
    void testGetBestSelectorEmptyElement() {
        DomElement element = createElement("div", Map.of());
        SelectorStrategy best = scorer.getBestSelector(element);
        assertNotNull(best);
        // Should fall back to TAG_ONLY
        assertEquals(SelectorStrategy.Strategy.TAG_ONLY, best.getStrategy());
    }

    // --- Shadow DOM flag tests ---

    @Test
    void testShadowDomFlagPropagated() {
        Map<String, String> attrs = Map.of("data-testid", "inner-btn");
        DomElement element = createElement("button", attrs);

        List<SelectorStrategy> candidates = scorer.generateAndScoreCandidates(element, true);
        assertTrue(candidates.stream().allMatch(SelectorStrategy::isRequiresShadowPiercing));
    }

    @Test
    void testNonShadowDomFlagPropagated() {
        Map<String, String> attrs = Map.of("id", "outer-btn");
        DomElement element = createElement("button", attrs);

        List<SelectorStrategy> candidates = scorer.generateAndScoreCandidates(element, false);
        assertTrue(candidates.stream().noneMatch(SelectorStrategy::isRequiresShadowPiercing));
    }

    // --- getBestPlaywrightSelector tests ---

    @Test
    void testGetBestPlaywrightSelectorNormal() {
        Map<String, String> attrs = Map.of("data-testid", "search-btn");
        DomElement element = createElement("button", attrs);

        String selector = scorer.getBestPlaywrightSelector(element, false, null);
        assertTrue(selector.contains("data-testid"));
        assertFalse(selector.contains(">>"));
    }

    @Test
    void testGetBestPlaywrightSelectorShadowDom() {
        Map<String, String> attrs = Map.of("data-testid", "inner-input");
        DomElement element = createElement("input", attrs);

        String selector = scorer.getBestPlaywrightSelector(element, true, "my-component");
        assertTrue(selector.contains(">>"));
        assertTrue(selector.contains("my-component"));
        assertTrue(selector.contains("data-testid"));
    }

    // --- Sorting / ordering tests ---

    @Test
    void testCandidatesSortedByScoreDescending() {
        Map<String, String> attrs = new HashMap<>();
        attrs.put("id", "my-btn");
        attrs.put("data-testid", "submit");
        attrs.put("name", "submit_btn");
        attrs.put("class", "btn primary");
        attrs.put("placeholder", "");
        attrs.put("type", "submit");

        DomElement element = createElement("button", attrs, "Submit Form", "button");
        List<SelectorStrategy> candidates = scorer.generateAndScoreCandidates(element);

        for (int i = 0; i < candidates.size() - 1; i++) {
            assertTrue(candidates.get(i).getScore() >= candidates.get(i + 1).getScore(),
                    "Candidates should be sorted by score descending");
        }
    }

    // --- SelectorStrategy tests ---

    @Test
    void testSelectorStrategyProperties() {
        SelectorStrategy s = new SelectorStrategy("#btn", SelectorStrategy.Strategy.ID, 85.0);
        assertEquals("#btn", s.getSelector());
        assertEquals(SelectorStrategy.Strategy.ID, s.getStrategy());
        assertEquals(85.0, s.getScore(), 0.001);
        assertFalse(s.isRequiresShadowPiercing());
    }

    @Test
    void testSelectorStrategyShadowPiercing() {
        SelectorStrategy s = new SelectorStrategy("[data-testid=\"x\"]",
                SelectorStrategy.Strategy.DATA_TEST_ID, 95.0, true);
        assertTrue(s.isRequiresShadowPiercing());
    }

    @Test
    void testSelectorStrategyToString() {
        SelectorStrategy s = new SelectorStrategy("#btn", SelectorStrategy.Strategy.ID, 85.0);
        String str = s.toString();
        assertTrue(str.contains("ID"));
        assertTrue(str.contains("#btn"));
        assertTrue(str.contains("85"));
    }

    @Test
    void testSelectorStrategyToStringShadow() {
        SelectorStrategy s = new SelectorStrategy("#btn", SelectorStrategy.Strategy.ID, 85.0, true);
        String str = s.toString();
        assertTrue(str.contains("shadow"));
    }
}
