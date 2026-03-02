package com.browserautomation.dom;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a single DOM element with its properties and attributes.
 * Elements are indexed for LLM reference (e.g., [1], [2], etc.).
 */
public class DomElement {

    private final int index;
    private final String tagName;
    private final Map<String, String> attributes;
    private final String textContent;
    private final boolean isVisible;
    private final boolean isInteractive;
    private final boolean isScrollable;
    private final String role;
    private final String ariaLabel;
    private final BoundingBox boundingBox;

    public DomElement(int index, String tagName, Map<String, String> attributes, String textContent,
                      boolean isVisible, boolean isInteractive, boolean isScrollable,
                      String role, String ariaLabel, BoundingBox boundingBox) {
        this.index = index;
        this.tagName = tagName;
        this.attributes = attributes != null ? attributes : new HashMap<>();
        this.textContent = textContent;
        this.isVisible = isVisible;
        this.isInteractive = isInteractive;
        this.isScrollable = isScrollable;
        this.role = role;
        this.ariaLabel = ariaLabel;
        this.boundingBox = boundingBox;
    }

    /**
     * Build the best scored selector using the SelectorScorer.
     * This evaluates multiple selector strategies and returns the highest-scoring one.
     *
     * @return the best selector string based on scoring
     */
    public String buildScoredSelector() {
        return buildScoredSelector(false);
    }

    /**
     * Build the best scored selector, optionally accounting for Shadow DOM.
     *
     * @param inShadowDom whether this element is inside a Shadow DOM
     * @return the best selector string based on scoring
     */
    public String buildScoredSelector(boolean inShadowDom) {
        SelectorScorer scorer = new SelectorScorer();
        List<SelectorStrategy> candidates = scorer.generateAndScoreCandidates(this, inShadowDom);
        if (!candidates.isEmpty()) {
            return candidates.get(0).getSelector();
        }
        return buildSelector();
    }

    /**
     * Get all scored selector candidates for this element.
     *
     * @return list of scored selectors, sorted by score descending
     */
    public List<SelectorStrategy> getScoredSelectors() {
        SelectorScorer scorer = new SelectorScorer();
        return scorer.generateAndScoreCandidates(this, false);
    }

    /**
     * Check if this element resides inside a Shadow DOM.
     */
    public boolean isInShadowDom() {
        return "true".equals(attributes.get("data-in-shadow-dom"));
    }

    /**
     * Get the shadow host selector if this element is inside a Shadow DOM.
     */
    public String getShadowHostSelector() {
        return attributes.get("data-shadow-host");
    }

    /**
     * Build a CSS selector to locate this element on the page.
     */
    public String buildSelector() {
        String id = attributes.get("id");
        if (id != null && !id.isEmpty()) {
            return "#" + cssEscape(id);
        }

        String name = attributes.get("name");
        if (name != null && !name.isEmpty()) {
            return tagName.toLowerCase() + "[name=\"" + escapeAttr(name) + "\"]";
        }

        String ariaLabelAttr = attributes.get("aria-label");
        if (ariaLabelAttr != null && !ariaLabelAttr.isEmpty()) {
            return tagName.toLowerCase() + "[aria-label=\"" + escapeAttr(ariaLabelAttr) + "\"]";
        }

        String placeholder = attributes.get("placeholder");
        if (placeholder != null && !placeholder.isEmpty()) {
            return tagName.toLowerCase() + "[placeholder=\"" + escapeAttr(placeholder) + "\"]";
        }

        String dataTestId = attributes.get("data-testid");
        if (dataTestId != null && !dataTestId.isEmpty()) {
            return "[data-testid=\"" + escapeAttr(dataTestId) + "\"]";
        }

        // Fallback: use tag + text content
        String type = attributes.get("type");
        if (type != null && !type.isEmpty()) {
            return tagName.toLowerCase() + "[type=\"" + escapeAttr(type) + "\"]";
        }

        return tagName.toLowerCase();
    }

    /**
     * Get a human-readable description for logging.
     */
    public String getDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append("<").append(tagName.toLowerCase());

        if (role != null && !role.isEmpty()) {
            sb.append(" role=").append(role);
        }
        if (ariaLabel != null && !ariaLabel.isEmpty()) {
            sb.append(" aria-label=\"").append(truncate(ariaLabel, 30)).append("\"");
        }

        String id = attributes.get("id");
        if (id != null && !id.isEmpty()) {
            sb.append(" id=\"").append(id).append("\"");
        }

        sb.append(">");

        if (textContent != null && !textContent.isEmpty()) {
            sb.append(" \"").append(truncate(textContent, 40)).append("\"");
        }

        return sb.toString();
    }

    /**
     * Serialize this element to a string representation for the LLM.
     */
    public String toLlmRepresentation() {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(index).append("] ");
        sb.append("<").append(tagName.toLowerCase());

        // Include relevant attributes
        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (isRelevantAttribute(key) && value != null && !value.isEmpty()) {
                sb.append(" ").append(key).append("=\"").append(truncate(value, 50)).append("\"");
            }
        }

        if (role != null && !role.isEmpty() && !attributes.containsKey("role")) {
            sb.append(" role=\"").append(role).append("\"");
        }

        sb.append(">");

        if (textContent != null && !textContent.trim().isEmpty()) {
            sb.append(truncate(textContent.trim(), 80));
        }

        sb.append("</").append(tagName.toLowerCase()).append(">");

        return sb.toString();
    }

    private boolean isRelevantAttribute(String key) {
        return key.equals("id") || key.equals("name") || key.equals("type") ||
                key.equals("value") || key.equals("placeholder") || key.equals("aria-label") ||
                key.equals("href") || key.equals("src") || key.equals("alt") ||
                key.equals("title") || key.equals("class") || key.equals("role") ||
                key.equals("checked") || key.equals("disabled") || key.equals("selected") ||
                key.equals("aria-expanded") || key.equals("aria-checked") ||
                key.equals("data-testid") || key.equals("required");
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength) + "...";
    }

    private String cssEscape(String value) {
        return value.replaceAll("([\\\\!\"#$%&'()*+,./:;<=>?@\\[\\]^`{|}~])", "\\\\$1");
    }

    private String escapeAttr(String value) {
        return value.replace("\"", "\\\"");
    }

    // Getters

    public int getIndex() {
        return index;
    }

    public String getTagName() {
        return tagName;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public String getTextContent() {
        return textContent;
    }

    public boolean isVisible() {
        return isVisible;
    }

    public boolean isInteractive() {
        return isInteractive;
    }

    public boolean isScrollable() {
        return isScrollable;
    }

    public String getRole() {
        return role;
    }

    public String getAriaLabel() {
        return ariaLabel;
    }

    public BoundingBox getBoundingBox() {
        return boundingBox;
    }

    /**
     * Bounding box coordinates of the element.
     */
    public static class BoundingBox {
        private final double x;
        private final double y;
        private final double width;
        private final double height;

        public BoundingBox(double x, double y, double width, double height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        public double getX() { return x; }
        public double getY() { return y; }
        public double getWidth() { return width; }
        public double getHeight() { return height; }
    }
}
