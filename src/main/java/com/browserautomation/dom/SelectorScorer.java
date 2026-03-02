package com.browserautomation.dom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Scores and ranks selector candidates for DOM elements.
 *
 * Generates multiple selector strategies for each element and scores them
 * based on uniqueness, stability, readability, and specificity. This ensures
 * robust element identification in Angular, React, and other SPA frameworks
 * that use Shadow DOM.
 *
 * Scoring criteria:
 * - Uniqueness (0-40): How likely the selector matches exactly one element
 * - Stability (0-30): How resistant the selector is to UI refactoring
 * - Readability (0-15): How easy it is for a human to understand
 * - Specificity (0-15): How precise the selector is without being fragile
 */
public class SelectorScorer {

    private static final Logger logger = LoggerFactory.getLogger(SelectorScorer.class);

    // Weight constants for scoring dimensions
    private static final double UNIQUENESS_WEIGHT = 40.0;
    private static final double STABILITY_WEIGHT = 30.0;
    private static final double READABILITY_WEIGHT = 15.0;
    private static final double SPECIFICITY_WEIGHT = 15.0;

    // Known unstable class prefixes (CSS-in-JS, Angular, React generated)
    private static final List<String> UNSTABLE_CLASS_PREFIXES = List.of(
            "ng-", "_ng", "cdk-", "mat-",      // Angular
            "css-", "sc-", "styled-",           // CSS-in-JS / Styled Components
            "MuiGrid", "MuiPaper", "Mui",       // Material UI
            "chakra-",                           // Chakra UI
            "ant-",                              // Ant Design
            "bp3-", "bp4-",                      // Blueprint
            "tw-",                               // Tailwind generated
            "jsx-", "__next",                    // Next.js
            "svelte-"                            // Svelte
    );

    /**
     * Generate and score all possible selector candidates for a DomElement.
     *
     * @param element the element to generate selectors for
     * @return list of scored selector strategies, sorted by score descending
     */
    public List<SelectorStrategy> generateAndScoreCandidates(DomElement element) {
        return generateAndScoreCandidates(element, false);
    }

    /**
     * Generate and score all possible selector candidates for a DomElement.
     *
     * @param element      the element to generate selectors for
     * @param inShadowDom  whether the element is inside a shadow DOM
     * @return list of scored selector strategies, sorted by score descending
     */
    public List<SelectorStrategy> generateAndScoreCandidates(DomElement element, boolean inShadowDom) {
        List<SelectorStrategy> candidates = new ArrayList<>();
        Map<String, String> attrs = element.getAttributes();
        String tag = element.getTagName().toLowerCase();

        // Strategy 1: ID selector
        String id = attrs.get("id");
        if (id != null && !id.isEmpty() && !isGeneratedId(id)) {
            double score = scoreIdSelector(id);
            candidates.add(new SelectorStrategy("#" + cssEscape(id),
                    SelectorStrategy.Strategy.ID, score, inShadowDom));
        }

        // Strategy 2: data-testid (most stable for test automation)
        String testId = attrs.get("data-testid");
        if (testId == null) testId = attrs.get("data-test-id");
        if (testId == null) testId = attrs.get("data-cy");
        if (testId == null) testId = attrs.get("data-test");
        if (testId != null && !testId.isEmpty()) {
            String attrName = attrs.containsKey("data-testid") ? "data-testid" :
                    attrs.containsKey("data-test-id") ? "data-test-id" :
                            attrs.containsKey("data-cy") ? "data-cy" : "data-test";
            double score = scoreDataTestId(testId);
            candidates.add(new SelectorStrategy("[" + attrName + "=\"" + escapeAttr(testId) + "\"]",
                    SelectorStrategy.Strategy.DATA_TEST_ID, score, inShadowDom));
        }

        // Strategy 3: aria-label
        String ariaLabel = attrs.get("aria-label");
        if (ariaLabel != null && !ariaLabel.isEmpty()) {
            double score = scoreAriaLabel(ariaLabel);
            candidates.add(new SelectorStrategy(
                    tag + "[aria-label=\"" + escapeAttr(ariaLabel) + "\"]",
                    SelectorStrategy.Strategy.ARIA_LABEL, score, inShadowDom));
        }

        // Strategy 4: ARIA role + text content
        String role = element.getRole();
        String text = element.getTextContent();
        if (role != null && !role.isEmpty() && text != null && !text.isEmpty() && text.length() <= 60) {
            double score = scoreAriaRoleText(role, text);
            candidates.add(new SelectorStrategy(
                    tag + "[role=\"" + escapeAttr(role) + "\"]",
                    SelectorStrategy.Strategy.ARIA_ROLE_TEXT, score, inShadowDom));
        }

        // Strategy 5: name attribute
        String name = attrs.get("name");
        if (name != null && !name.isEmpty()) {
            double score = scoreNameAttribute(name, tag);
            candidates.add(new SelectorStrategy(
                    tag + "[name=\"" + escapeAttr(name) + "\"]",
                    SelectorStrategy.Strategy.NAME, score, inShadowDom));
        }

        // Strategy 6: placeholder attribute
        String placeholder = attrs.get("placeholder");
        if (placeholder != null && !placeholder.isEmpty()) {
            double score = scorePlaceholder(placeholder);
            candidates.add(new SelectorStrategy(
                    tag + "[placeholder=\"" + escapeAttr(placeholder) + "\"]",
                    SelectorStrategy.Strategy.PLACEHOLDER, score, inShadowDom));
        }

        // Strategy 7: CSS class combination (filter out dynamic/generated classes)
        String className = attrs.get("class");
        if (className != null && !className.isEmpty()) {
            List<String> stableClasses = filterStableClasses(className);
            if (!stableClasses.isEmpty()) {
                String classSelector = tag + stableClasses.stream()
                        .map(c -> "." + cssEscape(c))
                        .reduce("", String::concat);
                double score = scoreCssClasses(stableClasses, tag);
                candidates.add(new SelectorStrategy(classSelector,
                        SelectorStrategy.Strategy.CSS_CLASS, score, inShadowDom));
            }
        }

        // Strategy 8: Tag + type attribute (for inputs)
        String type = attrs.get("type");
        if (type != null && !type.isEmpty()) {
            double score = scoreTagType(tag, type);
            candidates.add(new SelectorStrategy(
                    tag + "[type=\"" + escapeAttr(type) + "\"]",
                    SelectorStrategy.Strategy.TAG_TYPE, score, inShadowDom));
        }

        // Strategy 9: Text content (Playwright text= selector)
        if (text != null && !text.isEmpty() && text.length() <= 60 && text.length() >= 2) {
            double score = scoreTextContent(text);
            candidates.add(new SelectorStrategy(
                    "text=\"" + escapeAttr(text.trim()) + "\"",
                    SelectorStrategy.Strategy.TEXT_CONTENT, score, inShadowDom));
        }

        // Strategy 10: Custom data attributes
        for (Map.Entry<String, String> attr : attrs.entrySet()) {
            String key = attr.getKey();
            String value = attr.getValue();
            if (key.startsWith("data-") && !key.equals("data-testid") && !key.equals("data-test-id")
                    && !key.equals("data-cy") && !key.equals("data-test")
                    && value != null && !value.isEmpty() && value.length() <= 80) {
                double score = scoreDataAttribute(key, value);
                candidates.add(new SelectorStrategy(
                        "[" + key + "=\"" + escapeAttr(value) + "\"]",
                        SelectorStrategy.Strategy.DATA_ATTRIBUTE, score, inShadowDom));
            }
        }

        // Strategy 11: Tag-only fallback
        double tagScore = scoreTagOnly(tag);
        candidates.add(new SelectorStrategy(tag,
                SelectorStrategy.Strategy.TAG_ONLY, tagScore, inShadowDom));

        // Sort by score descending
        candidates.sort(Comparator.comparingDouble(SelectorStrategy::getScore).reversed());

        if (!candidates.isEmpty()) {
            logger.debug("Best selector for element [{}]: {} (score: {:.2f})",
                    element.getIndex(), candidates.get(0).getSelector(), candidates.get(0).getScore());
        }

        return candidates;
    }

    /**
     * Get the best selector for a given element.
     *
     * @param element the element to select
     * @return the highest-scoring selector
     */
    public SelectorStrategy getBestSelector(DomElement element) {
        return getBestSelector(element, false);
    }

    /**
     * Get the best selector for a given element.
     *
     * @param element     the element to select
     * @param inShadowDom whether the element is inside shadow DOM
     * @return the highest-scoring selector
     */
    public SelectorStrategy getBestSelector(DomElement element, boolean inShadowDom) {
        List<SelectorStrategy> candidates = generateAndScoreCandidates(element, inShadowDom);
        return candidates.isEmpty() ? null : candidates.get(0);
    }

    /**
     * Get the best Playwright-compatible selector string for a given element.
     * If the element is in a shadow DOM, returns a piercing selector.
     *
     * @param element     the element to select
     * @param inShadowDom whether the element is inside shadow DOM
     * @param shadowHost  the CSS selector of the shadow host (if applicable)
     * @return the best Playwright selector string
     */
    public String getBestPlaywrightSelector(DomElement element, boolean inShadowDom, String shadowHost) {
        SelectorStrategy best = getBestSelector(element, inShadowDom);
        if (best == null) {
            return element.getTagName().toLowerCase();
        }

        String selector = best.getSelector();

        // For Shadow DOM: Playwright supports >> for shadow piercing
        if (inShadowDom && shadowHost != null && !shadowHost.isEmpty()) {
            return shadowHost + " >> " + selector;
        }

        return selector;
    }

    // --- Scoring methods ---

    private double scoreIdSelector(String id) {
        double uniqueness = 38.0; // IDs are usually unique
        double stability = isGeneratedId(id) ? 5.0 : 25.0;
        double readability = 14.0;
        double specificity = 14.0;

        // Penalize long or random-looking IDs
        if (id.length() > 30) stability -= 10.0;
        if (id.matches(".*[0-9a-f]{8,}.*")) stability -= 15.0; // Looks like a hash

        return Math.max(0, uniqueness + stability + readability + specificity);
    }

    private double scoreDataTestId(String testId) {
        // Test IDs are the gold standard for automation
        double uniqueness = 40.0;
        double stability = 30.0; // Explicitly added for testing, very stable
        double readability = 14.0;
        double specificity = 14.0;
        return uniqueness + stability + readability + specificity; // ~98
    }

    private double scoreAriaLabel(String ariaLabel) {
        double uniqueness = 32.0;
        double stability = 24.0; // Semantic, but could change with i18n
        double readability = 13.0;
        double specificity = 12.0;

        // Penalize very long labels
        if (ariaLabel.length() > 60) readability -= 5.0;

        return Math.max(0, uniqueness + stability + readability + specificity);
    }

    private double scoreAriaRoleText(String role, String text) {
        double uniqueness = 25.0; // Role + text combo is reasonably unique
        double stability = 20.0;
        double readability = 12.0;
        double specificity = 10.0;

        // Generic roles reduce uniqueness
        if (role.equals("button") || role.equals("link")) uniqueness -= 5.0;

        return Math.max(0, uniqueness + stability + readability + specificity);
    }

    private double scoreNameAttribute(String name, String tag) {
        double uniqueness = 35.0; // Usually unique within form
        double stability = 26.0; // Form fields keep names for submission
        double readability = 13.0;
        double specificity = 12.0;

        // Names are most meaningful on form elements
        if (!tag.equals("input") && !tag.equals("select") && !tag.equals("textarea")) {
            stability -= 8.0;
        }

        return Math.max(0, uniqueness + stability + readability + specificity);
    }

    private double scorePlaceholder(String placeholder) {
        double uniqueness = 28.0;
        double stability = 18.0; // Placeholder text can change
        double readability = 14.0;
        double specificity = 10.0;

        if (placeholder.length() > 40) readability -= 5.0;

        return Math.max(0, uniqueness + stability + readability + specificity);
    }

    private double scoreCssClasses(List<String> stableClasses, String tag) {
        double uniqueness = 20.0 + Math.min(stableClasses.size() * 5, 15);
        double stability = 15.0; // Classes can change, but stable ones less so
        double readability = 10.0;
        double specificity = 10.0;

        // Having multiple stable classes increases confidence
        if (stableClasses.size() >= 2) stability += 5.0;

        return Math.max(0, uniqueness + stability + readability + specificity);
    }

    private double scoreTagType(String tag, String type) {
        double uniqueness = 15.0; // Many inputs share type
        double stability = 22.0; // Tag + type is reasonably stable
        double readability = 12.0;
        double specificity = 8.0;

        return Math.max(0, uniqueness + stability + readability + specificity);
    }

    private double scoreTextContent(String text) {
        double uniqueness = 22.0;
        double stability = 14.0; // Text can change with i18n or updates
        double readability = 14.0; // Very readable
        double specificity = 8.0;

        // Short, unique text is better
        if (text.length() > 30) readability -= 5.0;
        if (text.length() < 5) uniqueness -= 5.0;

        return Math.max(0, uniqueness + stability + readability + specificity);
    }

    private double scoreDataAttribute(String key, String value) {
        double uniqueness = 30.0;
        double stability = 22.0; // Custom data attributes are fairly stable
        double readability = 10.0;
        double specificity = 12.0;

        return Math.max(0, uniqueness + stability + readability + specificity);
    }

    private double scoreTagOnly(String tag) {
        double uniqueness = 3.0; // Almost never unique
        double stability = 20.0; // Tags don't change
        double readability = 12.0;
        double specificity = 2.0;

        return Math.max(0, uniqueness + stability + readability + specificity);
    }

    // --- Helper methods ---

    /**
     * Detect whether an ID looks auto-generated (Angular, React, etc.).
     */
    static boolean isGeneratedId(String id) {
        if (id == null || id.isEmpty()) return true;

        // Angular CDK IDs: cdk-*, mat-*
        if (id.matches("(?i)(cdk|mat|ng|_ng)-.*")) return true;
        // React/random hash IDs: contains long hex sequences
        if (id.matches(".*[0-9a-f]{8,}.*")) return true;
        // UUID-like
        if (id.matches("[0-9a-f]{8}-[0-9a-f]{4}-.*")) return true;
        // Numeric only
        if (id.matches("\\d+")) return true;
        // Auto-generated patterns: `:r0:`, `:r1:`, etc.
        if (id.matches(":[a-z0-9]+:")) return true;
        // Very long IDs are usually generated
        if (id.length() > 40) return true;

        return false;
    }

    /**
     * Filter out dynamically generated CSS classes.
     * Returns only classes that are likely to be stable across renders.
     */
    static List<String> filterStableClasses(String className) {
        List<String> stable = new ArrayList<>();
        if (className == null || className.isEmpty()) return stable;

        for (String cls : className.trim().split("\\s+")) {
            if (cls.isEmpty()) continue;
            if (isStableClass(cls)) {
                stable.add(cls);
            }
        }
        return stable;
    }

    /**
     * Check if a CSS class looks stable (not auto-generated).
     */
    static boolean isStableClass(String cls) {
        // Skip known unstable prefixes
        for (String prefix : UNSTABLE_CLASS_PREFIXES) {
            if (cls.startsWith(prefix)) return false;
        }

        // Skip classes that look like hashes: random letters/numbers
        if (cls.matches("[a-zA-Z]{1,3}[0-9a-zA-Z]{5,}") && !cls.contains("-") && !cls.contains("_")) {
            return false;
        }

        // Skip very short random-looking classes
        if (cls.length() <= 2 && cls.matches("[a-z]{1,2}")) return false;

        // Skip classes with long hex-like suffixes
        if (cls.matches(".*-[0-9a-f]{6,}")) return false;
        if (cls.matches(".*_[0-9a-f]{6,}")) return false;

        return true;
    }

    private String cssEscape(String value) {
        return value.replaceAll("([\\\\!\"#$%&'()*+,./:;<=>?@\\[\\]^`{|}~])", "\\\\$1");
    }

    private String escapeAttr(String value) {
        return value.replace("\"", "\\\"");
    }
}
