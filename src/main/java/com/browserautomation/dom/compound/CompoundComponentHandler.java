package com.browserautomation.dom.compound;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Handles compound/complex HTML components that require special interaction patterns.
 * Supports &lt;select&gt;, &lt;input type="date/time"&gt;, &lt;details&gt;/&lt;summary&gt;, and
 * other compound elements that cannot be interacted with via simple click/type.
 *
 */
public class CompoundComponentHandler {

    private static final Logger logger = LoggerFactory.getLogger(CompoundComponentHandler.class);

    private static final Set<String> DATE_TIME_TYPES = Set.of(
            "date", "time", "datetime-local", "month", "week"
    );

    /**
     * Detect compound components in a list of DOM elements.
     *
     * @param elements the raw element data from DOM extraction
     * @return list of detected compound components
     */
    public List<CompoundComponent> detectComponents(List<Map<String, Object>> elements) {
        List<CompoundComponent> components = new ArrayList<>();

        for (Map<String, Object> element : elements) {
            String tagName = getStringValue(element, "tagName", "").toUpperCase();
            Map<String, String> attributes = getAttributes(element);

            CompoundComponent component = detectComponent(tagName, attributes, element);
            if (component != null) {
                components.add(component);
            }
        }

        logger.debug("Detected {} compound components", components.size());
        return components;
    }

    /**
     * Detect if a single element is a compound component.
     */
    private CompoundComponent detectComponent(String tagName, Map<String, String> attributes,
                                               Map<String, Object> rawElement) {
        return switch (tagName) {
            case "SELECT" -> detectSelectComponent(attributes, rawElement);
            case "INPUT" -> detectInputComponent(attributes, rawElement);
            case "DETAILS" -> detectDetailsComponent(attributes, rawElement);
            default -> null;
        };
    }

    /**
     * Detect and analyze a &lt;select&gt; element.
     */
    private CompoundComponent detectSelectComponent(Map<String, String> attributes,
                                                     Map<String, Object> rawElement) {
        CompoundComponent component = new CompoundComponent(
                CompoundComponent.Type.SELECT, "SELECT", attributes);

        // Extract options if available
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> options = (List<Map<String, Object>>) rawElement.get("options");
        if (options != null) {
            for (Map<String, Object> opt : options) {
                String value = getStringValue(opt, "value", "");
                String text = getStringValue(opt, "text", "");
                boolean selected = getBooleanValue(opt, "selected", false);
                component.addOption(new CompoundComponent.Option(value, text, selected));
            }
        }

        boolean isMultiple = attributes.containsKey("multiple");
        component.setProperty("multiple", String.valueOf(isMultiple));

        String size = attributes.get("size");
        if (size != null) {
            component.setProperty("size", size);
        }

        return component;
    }

    /**
     * Detect and analyze date/time input elements.
     */
    private CompoundComponent detectInputComponent(Map<String, String> attributes,
                                                    Map<String, Object> rawElement) {
        String type = attributes.getOrDefault("type", "text").toLowerCase();

        if (DATE_TIME_TYPES.contains(type)) {
            CompoundComponent component = new CompoundComponent(
                    CompoundComponent.Type.DATE_TIME_INPUT, "INPUT", attributes);
            component.setProperty("inputType", type);

            String min = attributes.get("min");
            String max = attributes.get("max");
            String step = attributes.get("step");
            String value = attributes.get("value");

            if (min != null) component.setProperty("min", min);
            if (max != null) component.setProperty("max", max);
            if (step != null) component.setProperty("step", step);
            if (value != null) component.setProperty("currentValue", value);

            return component;
        }

        // Check for range input
        if ("range".equals(type)) {
            CompoundComponent component = new CompoundComponent(
                    CompoundComponent.Type.RANGE_INPUT, "INPUT", attributes);
            component.setProperty("inputType", "range");
            String min = attributes.getOrDefault("min", "0");
            String max = attributes.getOrDefault("max", "100");
            String step = attributes.getOrDefault("step", "1");
            String value = attributes.getOrDefault("value", "50");
            component.setProperty("min", min);
            component.setProperty("max", max);
            component.setProperty("step", step);
            component.setProperty("currentValue", value);
            return component;
        }

        // Check for color input
        if ("color".equals(type)) {
            CompoundComponent component = new CompoundComponent(
                    CompoundComponent.Type.COLOR_INPUT, "INPUT", attributes);
            component.setProperty("inputType", "color");
            String value = attributes.getOrDefault("value", "#000000");
            component.setProperty("currentValue", value);
            return component;
        }

        // Check for file input
        if ("file".equals(type)) {
            CompoundComponent component = new CompoundComponent(
                    CompoundComponent.Type.FILE_INPUT, "INPUT", attributes);
            component.setProperty("inputType", "file");
            String accept = attributes.get("accept");
            boolean multiple = attributes.containsKey("multiple");
            if (accept != null) component.setProperty("accept", accept);
            component.setProperty("multiple", String.valueOf(multiple));
            return component;
        }

        return null;
    }

    /**
     * Detect and analyze &lt;details&gt;/&lt;summary&gt; elements.
     */
    private CompoundComponent detectDetailsComponent(Map<String, String> attributes,
                                                      Map<String, Object> rawElement) {
        CompoundComponent component = new CompoundComponent(
                CompoundComponent.Type.DETAILS_SUMMARY, "DETAILS", attributes);

        boolean isOpen = attributes.containsKey("open");
        component.setProperty("open", String.valueOf(isOpen));

        String summaryText = getStringValue(rawElement, "summaryText", "");
        if (!summaryText.isEmpty()) {
            component.setProperty("summaryText", summaryText);
        }

        return component;
    }

    /**
     * Generate interaction instructions for a compound component.
     *
     * @param component the compound component
     * @return human-readable interaction instructions
     */
    public String getInteractionInstructions(CompoundComponent component) {
        return switch (component.getType()) {
            case SELECT -> getSelectInstructions(component);
            case DATE_TIME_INPUT -> getDateTimeInstructions(component);
            case DETAILS_SUMMARY -> getDetailsInstructions(component);
            case RANGE_INPUT -> getRangeInstructions(component);
            case COLOR_INPUT -> "Use select_dropdown or type a hex color value (e.g., #FF0000)";
            case FILE_INPUT -> "Use the file upload action to select files" +
                    (component.getOptions().isEmpty() ? "" : ". Accepted: " + component.getProperty("accept"));
        };
    }

    private String getSelectInstructions(CompoundComponent component) {
        StringBuilder sb = new StringBuilder("Use select_dropdown with one of these options:\n");
        for (CompoundComponent.Option opt : component.getOptions()) {
            sb.append("  - value=\"").append(opt.value()).append("\"");
            if (!opt.text().isEmpty()) {
                sb.append(" (").append(opt.text()).append(")");
            }
            if (opt.selected()) {
                sb.append(" [current]");
            }
            sb.append("\n");
        }
        boolean isMultiple = "true".equals(component.getProperty("multiple"));
        if (isMultiple) {
            sb.append("This is a multi-select: hold Ctrl/Cmd to select multiple options.");
        }
        return sb.toString();
    }

    private String getDateTimeInstructions(CompoundComponent component) {
        String type = component.getProperty("inputType");
        StringBuilder sb = new StringBuilder();
        sb.append("This is a ").append(type).append(" input. ");

        switch (type) {
            case "date" -> sb.append("Type a date in YYYY-MM-DD format.");
            case "time" -> sb.append("Type a time in HH:MM format.");
            case "datetime-local" -> sb.append("Type in YYYY-MM-DDTHH:MM format.");
            case "month" -> sb.append("Type in YYYY-MM format.");
            case "week" -> sb.append("Type in YYYY-Www format (e.g., 2024-W01).");
        }

        String min = component.getProperty("min");
        String max = component.getProperty("max");
        if (min != null || max != null) {
            sb.append(" Range: ");
            if (min != null) sb.append("min=").append(min);
            if (min != null && max != null) sb.append(", ");
            if (max != null) sb.append("max=").append(max);
            sb.append(".");
        }
        return sb.toString();
    }

    private String getDetailsInstructions(CompoundComponent component) {
        boolean isOpen = "true".equals(component.getProperty("open"));
        return "This is a collapsible section. Currently " +
                (isOpen ? "open" : "closed") +
                ". Click the summary to " + (isOpen ? "collapse" : "expand") + " it.";
    }

    private String getRangeInstructions(CompoundComponent component) {
        return String.format("This is a range slider (min=%s, max=%s, step=%s, current=%s). " +
                        "Use keyboard arrows or click to set value.",
                component.getProperty("min"),
                component.getProperty("max"),
                component.getProperty("step"),
                component.getProperty("currentValue"));
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> getAttributes(Map<String, Object> element) {
        Object attrs = element.get("attributes");
        if (attrs instanceof Map) {
            Map<String, String> result = new LinkedHashMap<>();
            ((Map<String, Object>) attrs).forEach((k, v) -> result.put(k, String.valueOf(v)));
            return result;
        }
        return Collections.emptyMap();
    }

    private String getStringValue(Map<String, Object> map, String key, String defaultValue) {
        Object value = map.get(key);
        return value != null ? String.valueOf(value) : defaultValue;
    }

    private boolean getBooleanValue(Map<String, Object> map, String key, boolean defaultValue) {
        Object value = map.get(key);
        if (value instanceof Boolean) return (Boolean) value;
        return defaultValue;
    }

    /**
     * Represents a detected compound component.
     */
    public static class CompoundComponent {
        public enum Type {
            SELECT, DATE_TIME_INPUT, DETAILS_SUMMARY,
            RANGE_INPUT, COLOR_INPUT, FILE_INPUT
        }

        private final Type type;
        private final String tagName;
        private final Map<String, String> attributes;
        private final Map<String, String> properties;
        private final List<Option> options;

        public CompoundComponent(Type type, String tagName, Map<String, String> attributes) {
            this.type = type;
            this.tagName = tagName;
            this.attributes = new LinkedHashMap<>(attributes);
            this.properties = new LinkedHashMap<>();
            this.options = new ArrayList<>();
        }

        public void setProperty(String key, String value) {
            properties.put(key, value);
        }

        public String getProperty(String key) {
            return properties.get(key);
        }

        public void addOption(Option option) {
            options.add(option);
        }

        public Type getType() { return type; }
        public String getTagName() { return tagName; }
        public Map<String, String> getAttributes() { return Collections.unmodifiableMap(attributes); }
        public Map<String, String> getProperties() { return Collections.unmodifiableMap(properties); }
        public List<Option> getOptions() { return Collections.unmodifiableList(options); }

        public record Option(String value, String text, boolean selected) {}

        @Override
        public String toString() {
            return String.format("CompoundComponent[type=%s, tag=%s, props=%s]", type, tagName, properties);
        }
    }
}
