package com.browserautomation.agent.output;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.*;

/**
 * Generic typed output schema for structured agent outputs.
 * Allows defining a schema that the LLM should conform to when producing results.
 * Equivalent to browser-use's StructuredOutputAction[T].
 *
 * Usage:
 * <pre>
 * StructuredOutputAction&lt;FlightResult&gt; output = new StructuredOutputAction&lt;&gt;(FlightResult.class);
 * Map&lt;String, Object&gt; schema = output.getJsonSchema();
 * FlightResult result = output.parse(llmJsonString);
 * </pre>
 *
 * @param <T> the output type
 */
public class StructuredOutputAction<T> {

    private static final Logger logger = LoggerFactory.getLogger(StructuredOutputAction.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final Class<T> outputType;
    private final String description;
    private final Map<String, Object> jsonSchema;

    public StructuredOutputAction(Class<T> outputType) {
        this(outputType, null);
    }

    public StructuredOutputAction(Class<T> outputType, String description) {
        this.outputType = outputType;
        this.description = description != null ? description : "Structured output of type " + outputType.getSimpleName();
        this.jsonSchema = generateSchema(outputType);
    }

    /**
     * Parse a JSON string into the output type.
     *
     * @param json the JSON string from LLM
     * @return the parsed object
     * @throws StructuredOutputException if parsing fails
     */
    public T parse(String json) {
        if (json == null || json.isEmpty()) {
            throw new StructuredOutputException("Empty JSON input");
        }
        try {
            // Try to extract JSON from markdown code blocks
            String cleanJson = extractJson(json);
            return objectMapper.readValue(cleanJson, outputType);
        } catch (Exception e) {
            throw new StructuredOutputException("Failed to parse structured output: " + e.getMessage(), e);
        }
    }

    /**
     * Validate a JSON string against the schema.
     *
     * @param json the JSON string
     * @return list of validation errors (empty if valid)
     */
    public List<String> validate(String json) {
        List<String> errors = new ArrayList<>();
        if (json == null || json.isEmpty()) {
            errors.add("JSON input is null or empty");
            return errors;
        }
        try {
            String cleanJson = extractJson(json);
            JsonNode node = objectMapper.readTree(cleanJson);

            // Check required fields
            for (Field field : outputType.getDeclaredFields()) {
                String fieldName = field.getName();
                if (!node.has(fieldName)) {
                    // Check if it's a primitive (required by default)
                    if (field.getType().isPrimitive()) {
                        errors.add("Missing required field: " + fieldName);
                    }
                }
            }

            // Try full parse
            objectMapper.readValue(cleanJson, outputType);
        } catch (Exception e) {
            errors.add("Parse error: " + e.getMessage());
        }
        return errors;
    }

    /**
     * Get the JSON schema for this output type.
     *
     * @return the schema as a map
     */
    public Map<String, Object> getJsonSchema() {
        return Collections.unmodifiableMap(jsonSchema);
    }

    /**
     * Get the tool definition for this structured output.
     * Can be used as a tool definition for the LLM.
     *
     * @return the tool definition map
     */
    public Map<String, Object> getToolDefinition() {
        Map<String, Object> tool = new LinkedHashMap<>();
        tool.put("type", "function");
        Map<String, Object> function = new LinkedHashMap<>();
        function.put("name", "structured_output");
        function.put("description", description);
        function.put("parameters", jsonSchema);
        tool.put("function", function);
        return tool;
    }

    /**
     * Get the output type class.
     */
    public Class<T> getOutputType() {
        return outputType;
    }

    /**
     * Get the description.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Generate a JSON schema from a Java class.
     */
    private Map<String, Object> generateSchema(Class<?> clazz) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");

        Map<String, Object> properties = new LinkedHashMap<>();
        List<String> required = new ArrayList<>();

        for (Field field : clazz.getDeclaredFields()) {
            String fieldName = field.getName();
            Map<String, Object> fieldSchema = getFieldSchema(field.getType());
            properties.put(fieldName, fieldSchema);

            if (field.getType().isPrimitive()) {
                required.add(fieldName);
            }
        }

        schema.put("properties", properties);
        if (!required.isEmpty()) {
            schema.put("required", required);
        }

        return schema;
    }

    private Map<String, Object> getFieldSchema(Class<?> type) {
        Map<String, Object> schema = new LinkedHashMap<>();

        if (type == String.class) {
            schema.put("type", "string");
        } else if (type == int.class || type == Integer.class) {
            schema.put("type", "integer");
        } else if (type == long.class || type == Long.class) {
            schema.put("type", "integer");
        } else if (type == double.class || type == Double.class || type == float.class || type == Float.class) {
            schema.put("type", "number");
        } else if (type == boolean.class || type == Boolean.class) {
            schema.put("type", "boolean");
        } else if (type == List.class) {
            schema.put("type", "array");
            schema.put("items", Map.of("type", "string"));
        } else if (type == Map.class) {
            schema.put("type", "object");
        } else {
            // Nested object
            schema.putAll(generateSchema(type));
        }

        return schema;
    }

    /**
     * Extract JSON from text that may contain markdown code blocks.
     */
    private String extractJson(String text) {
        String trimmed = text.trim();

        // Try extracting from ```json ... ``` blocks
        if (trimmed.contains("```json")) {
            int start = trimmed.indexOf("```json") + 7;
            int end = trimmed.indexOf("```", start);
            if (end > start) {
                return trimmed.substring(start, end).trim();
            }
        }

        // Try extracting from ``` ... ``` blocks
        if (trimmed.startsWith("```") && trimmed.endsWith("```")) {
            return trimmed.substring(3, trimmed.length() - 3).trim();
        }

        // Try finding JSON object or array
        int braceStart = trimmed.indexOf('{');
        int bracketStart = trimmed.indexOf('[');

        if (braceStart >= 0 && (bracketStart < 0 || braceStart < bracketStart)) {
            int braceEnd = trimmed.lastIndexOf('}');
            if (braceEnd > braceStart) {
                return trimmed.substring(braceStart, braceEnd + 1);
            }
        }

        if (bracketStart >= 0) {
            int bracketEnd = trimmed.lastIndexOf(']');
            if (bracketEnd > bracketStart) {
                return trimmed.substring(bracketStart, bracketEnd + 1);
            }
        }

        return trimmed;
    }

    /**
     * Exception for structured output parsing/validation failures.
     */
    public static class StructuredOutputException extends RuntimeException {
        public StructuredOutputException(String message) {
            super(message);
        }

        public StructuredOutputException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
