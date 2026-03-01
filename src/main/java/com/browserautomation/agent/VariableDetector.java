package com.browserautomation.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Detects and extracts variables from conversation messages.
 * Equivalent to browser-use's agent/variable_detector.py.
 *
 * Identifies patterns like "the user's name is X", "set price to Y",
 * placeholders like {variable}, ${variable}, and key=value pairs.
 */
public class VariableDetector {

    private static final Logger logger = LoggerFactory.getLogger(VariableDetector.class);

    // Patterns for variable detection
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{(\\w+)}|\\{(\\w+)}");
    private static final Pattern KEY_VALUE_PATTERN = Pattern.compile("(\\w+)\\s*[:=]\\s*[\"']?([^\"',\\n]+)[\"']?");
    private static final Pattern ASSIGNMENT_PATTERN =
            Pattern.compile("(?:set|the|use|enter|type)\\s+(\\w[\\w\\s]*?)\\s+(?:is|to|as|=)\\s+[\"']?([^\"'\\n]+)[\"']?", Pattern.CASE_INSENSITIVE);
    private static final Pattern URL_PATTERN = Pattern.compile("(https?://\\S+)");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("([\\w.+-]+@[\\w-]+\\.[\\w.]+)");
    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\b(\\d+(?:\\.\\d+)?)\\b");

    private final Map<String, String> detectedVariables;

    public VariableDetector() {
        this.detectedVariables = new HashMap<>();
    }

    /**
     * Scan a message and extract any variables found.
     *
     * @param message the text to scan
     * @return map of variable names to values
     */
    public Map<String, String> detectVariables(String message) {
        Map<String, String> found = new HashMap<>();

        if (message == null || message.isEmpty()) {
            return found;
        }

        // Detect placeholders like ${var} or {var}
        Matcher placeholderMatcher = PLACEHOLDER_PATTERN.matcher(message);
        while (placeholderMatcher.find()) {
            String name = placeholderMatcher.group(1) != null ? placeholderMatcher.group(1) : placeholderMatcher.group(2);
            found.put(name, null); // Placeholder detected, value unknown
        }

        // Detect assignment patterns like "the username is john"
        Matcher assignmentMatcher = ASSIGNMENT_PATTERN.matcher(message);
        while (assignmentMatcher.find()) {
            String name = assignmentMatcher.group(1).trim().replaceAll("\\s+", "_").toLowerCase();
            String value = assignmentMatcher.group(2).trim();
            found.put(name, value);
        }

        // Detect URLs
        Matcher urlMatcher = URL_PATTERN.matcher(message);
        int urlCount = 0;
        while (urlMatcher.find()) {
            found.put("url" + (urlCount > 0 ? "_" + urlCount : ""), urlMatcher.group(1));
            urlCount++;
        }

        // Detect emails
        Matcher emailMatcher = EMAIL_PATTERN.matcher(message);
        int emailCount = 0;
        while (emailMatcher.find()) {
            found.put("email" + (emailCount > 0 ? "_" + emailCount : ""), emailMatcher.group(1));
            emailCount++;
        }

        // Store all detected variables
        for (Map.Entry<String, String> entry : found.entrySet()) {
            if (entry.getValue() != null) {
                detectedVariables.put(entry.getKey(), entry.getValue());
            }
        }

        if (!found.isEmpty()) {
            logger.debug("Detected {} variables in message", found.size());
        }

        return found;
    }

    /**
     * Extract key-value pairs from structured text.
     *
     * @param text the text to parse
     * @return map of extracted key-value pairs
     */
    public Map<String, String> extractKeyValuePairs(String text) {
        Map<String, String> pairs = new HashMap<>();
        if (text == null) return pairs;

        Matcher matcher = KEY_VALUE_PATTERN.matcher(text);
        while (matcher.find()) {
            pairs.put(matcher.group(1).trim(), matcher.group(2).trim());
        }

        return pairs;
    }

    /**
     * Substitute variables in a template string.
     *
     * @param template  the template with ${var} or {var} placeholders
     * @param variables map of variable names to values
     * @return the template with substituted values
     */
    public String substituteVariables(String template, Map<String, String> variables) {
        if (template == null) return null;

        String result = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            result = result.replace("${" + entry.getKey() + "}", entry.getValue());
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }

    /**
     * Get all detected variables accumulated over multiple calls.
     */
    public Map<String, String> getAllDetectedVariables() {
        return new HashMap<>(detectedVariables);
    }

    /**
     * Get a specific detected variable.
     */
    public String getVariable(String name) {
        return detectedVariables.get(name);
    }

    /**
     * Set a variable manually.
     */
    public void setVariable(String name, String value) {
        detectedVariables.put(name, value);
    }

    /**
     * Clear all detected variables.
     */
    public void clear() {
        detectedVariables.clear();
    }

    /**
     * Get the list of placeholder names found in a template.
     */
    public List<String> getPlaceholderNames(String template) {
        List<String> names = new ArrayList<>();
        if (template == null) return names;

        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);
        while (matcher.find()) {
            String name = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            if (!names.contains(name)) {
                names.add(name);
            }
        }
        return names;
    }
}
