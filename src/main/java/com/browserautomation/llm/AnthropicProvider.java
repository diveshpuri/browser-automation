package com.browserautomation.llm;

import com.browserautomation.config.BrowserAutomationConfig;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Anthropic Claude LLM provider.
 */
public class AnthropicProvider implements LlmProvider {

    private static final Logger logger = LoggerFactory.getLogger(AnthropicProvider.class);
    private static final MediaType JSON_TYPE = MediaType.parse("application/json");
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    private final String apiKey;
    private final String baseUrl;
    private final String model;
    private final double temperature;
    private final int maxTokens;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public AnthropicProvider(String apiKey, String model) {
        this(apiKey, "https://api.anthropic.com/v1", model, 0.0, 4096);
    }

    public AnthropicProvider(String apiKey, String baseUrl, String model, double temperature, int maxTokens) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.model = model;
        this.temperature = temperature;
        this.maxTokens = maxTokens;
        this.objectMapper = new ObjectMapper();
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    /**
     * Create from environment configuration.
     */
    public static AnthropicProvider fromConfig() {
        BrowserAutomationConfig config = BrowserAutomationConfig.getInstance();
        return new AnthropicProvider(
                config.getAnthropicApiKey(),
                config.getAnthropicBaseUrl(),
                config.getDefaultModel(),
                0.0,
                4096
        );
    }

    @Override
    public LlmResponse chatCompletion(List<ChatMessage> messages, List<Map<String, Object>> tools) {
        try {
            ObjectNode requestBody = buildRequestBody(messages, tools);
            String jsonBody = objectMapper.writeValueAsString(requestBody);

            logger.debug("Sending request to Anthropic API: model={}", model);

            Request request = new Request.Builder()
                    .url(baseUrl + "/messages")
                    .addHeader("x-api-key", apiKey)
                    .addHeader("anthropic-version", ANTHROPIC_VERSION)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(jsonBody, JSON_TYPE))
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                    throw new RuntimeException("Anthropic API error (HTTP " + response.code() + "): " + errorBody);
                }

                String responseBody = response.body().string();
                return parseResponse(responseBody);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to communicate with Anthropic API: " + e.getMessage(), e);
        }
    }

    private ObjectNode buildRequestBody(List<ChatMessage> messages, List<Map<String, Object>> tools) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", model);
        body.put("max_tokens", maxTokens);
        body.put("temperature", temperature);

        // Extract system message
        String systemMessage = null;
        List<ChatMessage> nonSystemMessages = new ArrayList<>();
        for (ChatMessage msg : messages) {
            if (msg.getRole() == ChatMessage.Role.SYSTEM) {
                systemMessage = msg.getText();
            } else {
                nonSystemMessages.add(msg);
            }
        }

        if (systemMessage != null) {
            body.put("system", systemMessage);
        }

        // Build messages array
        ArrayNode messagesArray = body.putArray("messages");
        for (ChatMessage msg : nonSystemMessages) {
            ObjectNode msgNode = messagesArray.addObject();
            msgNode.put("role", msg.getRole() == ChatMessage.Role.ASSISTANT ? "assistant" : "user");

            List<ChatMessage.ContentPart> contentParts = msg.getContent();
            if (contentParts.size() == 1 && contentParts.get(0).getType() == ChatMessage.ContentPart.Type.TEXT) {
                msgNode.put("content", contentParts.get(0).getText());
            } else {
                ArrayNode contentArray = msgNode.putArray("content");
                for (ChatMessage.ContentPart part : contentParts) {
                    ObjectNode partNode = contentArray.addObject();
                    if (part.getType() == ChatMessage.ContentPart.Type.TEXT) {
                        partNode.put("type", "text");
                        partNode.put("text", part.getText());
                    } else if (part.getType() == ChatMessage.ContentPart.Type.IMAGE) {
                        partNode.put("type", "image");
                        ObjectNode source = partNode.putObject("source");
                        source.put("type", "base64");
                        source.put("media_type", "image/png");
                        source.put("data", part.getBase64Image());
                    }
                }
            }
        }

        // Build tools array for Anthropic format
        if (tools != null && !tools.isEmpty()) {
            ArrayNode toolsArray = body.putArray("tools");
            for (Map<String, Object> tool : tools) {
                @SuppressWarnings("unchecked")
                Map<String, Object> function = (Map<String, Object>) tool.get("function");
                if (function != null) {
                    ObjectNode toolNode = toolsArray.addObject();
                    toolNode.put("name", (String) function.get("name"));
                    toolNode.put("description", (String) function.get("description"));

                    Object params = function.get("parameters");
                    if (params instanceof String) {
                        try {
                            toolNode.set("input_schema", objectMapper.readTree((String) params));
                        } catch (IOException e) {
                            logger.warn("Failed to parse parameter schema: {}", e.getMessage());
                        }
                    } else {
                        toolNode.set("input_schema", objectMapper.valueToTree(params));
                    }
                }
            }
        }

        return body;
    }

    private LlmResponse parseResponse(String responseBody) throws IOException {
        JsonNode root = objectMapper.readTree(responseBody);

        // Parse usage
        int promptTokens = 0;
        int completionTokens = 0;
        JsonNode usage = root.get("usage");
        if (usage != null) {
            promptTokens = usage.path("input_tokens").asInt(0);
            completionTokens = usage.path("output_tokens").asInt(0);
        }

        // Parse content blocks
        StringBuilder textContent = new StringBuilder();
        List<LlmResponse.ToolCall> toolCalls = new ArrayList<>();

        JsonNode content = root.get("content");
        if (content != null && content.isArray()) {
            for (JsonNode block : content) {
                String type = block.get("type").asText();
                if ("text".equals(type)) {
                    textContent.append(block.get("text").asText());
                } else if ("tool_use".equals(type)) {
                    String id = block.get("id").asText();
                    String name = block.get("name").asText();
                    JsonNode input = block.get("input");
                    Map<String, Object> args = objectMapper.convertValue(input, new TypeReference<>() {});
                    toolCalls.add(new LlmResponse.ToolCall(id, name, args));
                }
            }
        }

        logger.debug("Anthropic response: content_length={}, tool_calls={}, tokens={}",
                textContent.length(), toolCalls.size(), promptTokens + completionTokens);

        return new LlmResponse(textContent.toString(), toolCalls, promptTokens, completionTokens);
    }

    @Override
    public String getProviderName() {
        return "anthropic";
    }

    @Override
    public String getModelName() {
        return model;
    }

    @Override
    public boolean supportsVision() {
        return model.contains("claude-3") || model.contains("claude-sonnet") || model.contains("claude-opus");
    }
}
