package com.browserautomation.dom.serializer;

import com.browserautomation.dom.DomElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Multi-strategy DOM serializer for converting DOM elements into LLM-consumable formats.
 * Supports multiple serialization strategies with clickable element detection
 * and bounding box filtering.
 *
 * Equivalent to browser-use's DOM serializer (1276 lines).
 */
public class DomSerializer {

    private static final Logger logger = LoggerFactory.getLogger(DomSerializer.class);

    private final SerializationStrategy strategy;
    private final BoundingBoxFilter boundingBoxFilter;
    private final boolean includeNonInteractive;
    private final int maxElements;
    private final int maxTextLength;

    public DomSerializer() {
        this(SerializationStrategy.INDEXED, new BoundingBoxFilter(), false, 500, 200);
    }

    public DomSerializer(SerializationStrategy strategy, BoundingBoxFilter boundingBoxFilter,
                         boolean includeNonInteractive, int maxElements, int maxTextLength) {
        this.strategy = strategy;
        this.boundingBoxFilter = boundingBoxFilter;
        this.includeNonInteractive = includeNonInteractive;
        this.maxElements = maxElements;
        this.maxTextLength = maxTextLength;
    }

    /**
     * Serialize a list of DOM elements into a string representation.
     *
     * @param elements the DOM elements to serialize
     * @return the serialized string
     */
    public String serialize(List<DomElement> elements) {
        List<DomElement> filtered = filterElements(elements);
        return switch (strategy) {
            case INDEXED -> serializeIndexed(filtered);
            case MARKDOWN -> serializeMarkdown(filtered);
            case ACCESSIBILITY_TREE -> serializeAccessibilityTree(filtered);
            case COMPACT -> serializeCompact(filtered);
            case JSON -> serializeJson(filtered);
        };
    }

    /**
     * Detect clickable elements from a list of DOM elements.
     *
     * @param elements the DOM elements
     * @return list of elements that are clickable
     */
    public List<DomElement> detectClickableElements(List<DomElement> elements) {
        return elements.stream()
                .filter(this::isClickable)
                .collect(Collectors.toList());
    }

    /**
     * Check if an element is clickable.
     */
    public boolean isClickable(DomElement element) {
        // Check tag-based clickability
        String tag = element.getTagName().toUpperCase();
        if (Set.of("A", "BUTTON", "INPUT", "SELECT", "TEXTAREA").contains(tag)) {
            return true;
        }

        // Check role-based clickability
        String role = element.getRole();
        if (role != null && Set.of("button", "link", "tab", "menuitem", "checkbox",
                "radio", "switch", "option").contains(role)) {
            return true;
        }

        // Check attribute-based clickability
        Map<String, String> attrs = element.getAttributes();
        if (attrs.containsKey("onclick") || attrs.containsKey("tabindex") ||
                "true".equals(attrs.get("contenteditable"))) {
            return true;
        }

        // Check cursor style (if available)
        String cursor = attrs.get("style");
        if (cursor != null && cursor.contains("cursor: pointer")) {
            return true;
        }

        return false;
    }

    /**
     * Filter elements based on bounding box and visibility.
     */
    private List<DomElement> filterElements(List<DomElement> elements) {
        List<DomElement> filtered = new ArrayList<>();
        for (DomElement element : elements) {
            if (filtered.size() >= maxElements) break;

            if (!element.isVisible()) continue;
            if (!includeNonInteractive && !element.isInteractive()) continue;

            if (element.getBoundingBox() != null && !boundingBoxFilter.passes(element.getBoundingBox())) {
                continue;
            }

            filtered.add(element);
        }
        return filtered;
    }

    /**
     * Indexed serialization: "[index] tag text"
     */
    private String serializeIndexed(List<DomElement> elements) {
        StringBuilder sb = new StringBuilder();
        for (DomElement el : elements) {
            sb.append("[").append(el.getIndex()).append("] ");
            sb.append(formatElementTag(el));
            String text = truncateText(el.getTextContent());
            if (!text.isEmpty()) {
                sb.append(" ").append(text);
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    /**
     * Markdown serialization: structured markdown format.
     */
    private String serializeMarkdown(List<DomElement> elements) {
        StringBuilder sb = new StringBuilder();
        sb.append("| # | Element | Text | Clickable |\n");
        sb.append("|---|---------|------|----------|\n");
        for (DomElement el : elements) {
            sb.append("| ").append(el.getIndex()).append(" | ");
            sb.append(formatElementTag(el)).append(" | ");
            sb.append(truncateText(el.getTextContent())).append(" | ");
            sb.append(isClickable(el) ? "Yes" : "No").append(" |\n");
        }
        return sb.toString();
    }

    /**
     * Accessibility tree serialization: role-based hierarchical format.
     */
    private String serializeAccessibilityTree(List<DomElement> elements) {
        StringBuilder sb = new StringBuilder();
        for (DomElement el : elements) {
            String role = el.getRole() != null && !el.getRole().isEmpty()
                    ? el.getRole() : el.getTagName().toLowerCase();
            sb.append("[").append(el.getIndex()).append("] ");
            sb.append(role);

            String ariaLabel = el.getAriaLabel();
            if (ariaLabel != null && !ariaLabel.isEmpty()) {
                sb.append(" \"").append(ariaLabel).append("\"");
            } else {
                String text = truncateText(el.getTextContent());
                if (!text.isEmpty()) {
                    sb.append(" \"").append(text).append("\"");
                }
            }

            if (isClickable(el)) sb.append(" [clickable]");
            if (el.isScrollable()) sb.append(" [scrollable]");

            // Add state info
            Map<String, String> attrs = el.getAttributes();
            if ("true".equals(attrs.get("disabled"))) sb.append(" [disabled]");
            if ("true".equals(attrs.get("aria-expanded"))) sb.append(" [expanded]");
            if ("true".equals(attrs.get("aria-selected"))) sb.append(" [selected]");
            if (attrs.containsKey("aria-checked")) sb.append(" [checked=").append(attrs.get("aria-checked")).append("]");

            sb.append("\n");
        }
        return sb.toString();
    }

    /**
     * Compact serialization: minimal format for token efficiency.
     */
    private String serializeCompact(List<DomElement> elements) {
        StringBuilder sb = new StringBuilder();
        for (DomElement el : elements) {
            sb.append(el.getIndex()).append(":");
            sb.append(el.getTagName().toLowerCase());

            String text = truncateText(el.getTextContent());
            if (!text.isEmpty()) {
                sb.append("(").append(text).append(")");
            }

            // Add key attributes compactly
            Map<String, String> attrs = el.getAttributes();
            String href = attrs.get("href");
            if (href != null) sb.append("[href]");
            String type = attrs.get("type");
            if (type != null) sb.append("[").append(type).append("]");
            String placeholder = attrs.get("placeholder");
            if (placeholder != null) sb.append("{").append(truncateText(placeholder)).append("}");

            sb.append(" ");
        }
        return sb.toString().trim();
    }

    /**
     * JSON serialization: structured JSON format.
     */
    private String serializeJson(List<DomElement> elements) {
        StringBuilder sb = new StringBuilder("[\n");
        for (int i = 0; i < elements.size(); i++) {
            DomElement el = elements.get(i);
            sb.append("  {");
            sb.append("\"i\":").append(el.getIndex()).append(",");
            sb.append("\"tag\":\"").append(el.getTagName().toLowerCase()).append("\",");
            sb.append("\"text\":\"").append(escapeJson(truncateText(el.getTextContent()))).append("\",");
            sb.append("\"clickable\":").append(isClickable(el));

            if (el.getAriaLabel() != null && !el.getAriaLabel().isEmpty()) {
                sb.append(",\"label\":\"").append(escapeJson(el.getAriaLabel())).append("\"");
            }

            String role = el.getRole();
            if (role != null && !role.isEmpty()) {
                sb.append(",\"role\":\"").append(escapeJson(role)).append("\"");
            }

            sb.append("}");
            if (i < elements.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("]");
        return sb.toString();
    }

    private String formatElementTag(DomElement el) {
        StringBuilder sb = new StringBuilder();
        String tag = el.getTagName().toLowerCase();
        sb.append("<").append(tag);

        Map<String, String> attrs = el.getAttributes();
        // Include key attributes
        appendAttr(sb, attrs, "id");
        appendAttr(sb, attrs, "class");
        appendAttr(sb, attrs, "type");
        appendAttr(sb, attrs, "name");
        appendAttr(sb, attrs, "placeholder");
        appendAttr(sb, attrs, "href");
        appendAttr(sb, attrs, "role");
        appendAttr(sb, attrs, "aria-label");

        sb.append(">");
        return sb.toString();
    }

    private void appendAttr(StringBuilder sb, Map<String, String> attrs, String key) {
        String value = attrs.get(key);
        if (value != null && !value.isEmpty()) {
            String truncated = value.length() > 50 ? value.substring(0, 50) + "..." : value;
            sb.append(" ").append(key).append("=\"").append(truncated).append("\"");
        }
    }

    private String truncateText(String text) {
        if (text == null) return "";
        String trimmed = text.trim().replaceAll("\\s+", " ");
        if (trimmed.length() > maxTextLength) {
            return trimmed.substring(0, maxTextLength) + "...";
        }
        return trimmed;
    }

    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * Available serialization strategies.
     */
    public enum SerializationStrategy {
        /** Indexed format: "[0] <button> Click me" */
        INDEXED,
        /** Markdown table format */
        MARKDOWN,
        /** Accessibility tree format */
        ACCESSIBILITY_TREE,
        /** Minimal compact format for token efficiency */
        COMPACT,
        /** Structured JSON format */
        JSON
    }

    /**
     * Filter elements based on bounding box position and dimensions.
     */
    public static class BoundingBoxFilter {
        private double minWidth;
        private double minHeight;
        private double maxX;
        private double maxY;
        private boolean filterEnabled;

        public BoundingBoxFilter() {
            this.minWidth = 1.0;
            this.minHeight = 1.0;
            this.maxX = Double.MAX_VALUE;
            this.maxY = Double.MAX_VALUE;
            this.filterEnabled = true;
        }

        public BoundingBoxFilter(double minWidth, double minHeight, double maxX, double maxY) {
            this.minWidth = minWidth;
            this.minHeight = minHeight;
            this.maxX = maxX;
            this.maxY = maxY;
            this.filterEnabled = true;
        }

        public boolean passes(DomElement.BoundingBox bb) {
            if (!filterEnabled) return true;
            if (bb == null) return false;
            return bb.getWidth() >= minWidth &&
                    bb.getHeight() >= minHeight &&
                    bb.getX() <= maxX &&
                    bb.getY() <= maxY;
        }

        public void setFilterEnabled(boolean enabled) {
            this.filterEnabled = enabled;
        }

        public boolean isFilterEnabled() {
            return filterEnabled;
        }

        public double getMinWidth() { return minWidth; }
        public double getMinHeight() { return minHeight; }
        public double getMaxX() { return maxX; }
        public double getMaxY() { return maxY; }
    }

    /**
     * Builder for DomSerializer.
     */
    public static class Builder {
        private SerializationStrategy strategy = SerializationStrategy.INDEXED;
        private BoundingBoxFilter boundingBoxFilter = new BoundingBoxFilter();
        private boolean includeNonInteractive = false;
        private int maxElements = 500;
        private int maxTextLength = 200;

        public Builder strategy(SerializationStrategy strategy) {
            this.strategy = strategy;
            return this;
        }

        public Builder boundingBoxFilter(BoundingBoxFilter filter) {
            this.boundingBoxFilter = filter;
            return this;
        }

        public Builder includeNonInteractive(boolean include) {
            this.includeNonInteractive = include;
            return this;
        }

        public Builder maxElements(int max) {
            this.maxElements = max;
            return this;
        }

        public Builder maxTextLength(int max) {
            this.maxTextLength = max;
            return this;
        }

        public DomSerializer build() {
            return new DomSerializer(strategy, boundingBoxFilter, includeNonInteractive,
                    maxElements, maxTextLength);
        }
    }
}
