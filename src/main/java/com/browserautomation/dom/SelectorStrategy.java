package com.browserautomation.dom;

/**
 * Represents a single selector candidate with its generation strategy.
 * Used by the SelectorScorer to evaluate and rank selectors.
 */
public class SelectorStrategy {

    /**
     * The type/strategy used to generate this selector.
     */
    public enum Strategy {
        /** Element ID: #myId */
        ID,
        /** data-testid attribute: [data-testid="value"] */
        DATA_TEST_ID,
        /** ARIA label: [aria-label="value"] */
        ARIA_LABEL,
        /** ARIA role + text: role=button with text */
        ARIA_ROLE_TEXT,
        /** name attribute: input[name="value"] */
        NAME,
        /** placeholder attribute */
        PLACEHOLDER,
        /** CSS class combination: tag.class1.class2 */
        CSS_CLASS,
        /** Tag + type attribute: input[type="email"] */
        TAG_TYPE,
        /** Text content match (Playwright text selector) */
        TEXT_CONTENT,
        /** XPath based on text */
        XPATH_TEXT,
        /** nth-child positional: tag:nth-child(n) */
        NTH_CHILD,
        /** Full CSS path: parent > child > element */
        CSS_PATH,
        /** Custom data attribute: [data-*="value"] */
        DATA_ATTRIBUTE,
        /** Shadow DOM piercing: >>> combinator */
        SHADOW_PIERCE,
        /** Combination of tag + multiple attributes */
        COMPOUND,
        /** Fallback: tag name only */
        TAG_ONLY
    }

    private final String selector;
    private final Strategy strategy;
    private final double score;
    private final boolean requiresShadowPiercing;

    public SelectorStrategy(String selector, Strategy strategy, double score) {
        this(selector, strategy, score, false);
    }

    public SelectorStrategy(String selector, Strategy strategy, double score, boolean requiresShadowPiercing) {
        this.selector = selector;
        this.strategy = strategy;
        this.score = score;
        this.requiresShadowPiercing = requiresShadowPiercing;
    }

    public String getSelector() { return selector; }
    public Strategy getStrategy() { return strategy; }
    public double getScore() { return score; }
    public boolean isRequiresShadowPiercing() { return requiresShadowPiercing; }

    @Override
    public String toString() {
        return String.format("SelectorStrategy[%s: '%s' score=%.2f%s]",
                strategy, selector, score, requiresShadowPiercing ? " (shadow)" : "");
    }
}
