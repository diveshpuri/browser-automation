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
 * OpenAI-compatible LLM provider.
 * Works with OpenAI, Azure OpenAI, and any OpenAI-compatible API.
 */
public class OpenAiProvider implements LlmProvider {

    private static final Logger logger = LoggerFactory.getLogger(OpenAiProvider.class);
    private static final MediaType JSON_TYPE = MediaType.parse("application/json");

    private final String apiKey;
    private final String baseUrl;
    private final String model;
    private final double temperature;
    private final int maxTokens;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public OpenAiProvider(String apiKey, String model) {
        this(apiKey, "https://api.openai.com/v1", model, 0.0, 4096);
    }

    public OpenAiProvider(String apiKey, String baseUrl, String model, double temperature, int maxTokens) {
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
    public static OpenAiProvider fromConfig() {
        BrowserAutomationConfig config = BrowserAutomationConfig.getInstance();
        return new OpenAiProvider(
                config.getOpenAiApiKey(),
                config.getOpenAiBaseUrl(),
                config.getDefaultModel(),
                0.0,
                4096
        );
    }

    @Override
    public LlmResponse chatCompletion(List<ChatMessage> messages, List<Map<String, Object>> tools) {
        try {
            logger.info("[OPENAI] Request: model={}, messages={}, tools={}",
                    model, messages.size(), tools != null ? tools.size() : 0);

            ObjectNode requestBody = buildRequestBody(messages, tools);
            String jsonBody = objectMapper.writeValueAsString(requestBody);
            logger.info("[OPENAI] Request body size: {} bytes", jsonBody.length());

            Request request = new Request.Builder()
                    .url(baseUrl + "/chat/completions")
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(jsonBody, JSON_TYPE))
                    .build();

            long apiStart = System.currentTimeMillis();
            logger.info("[OPENAI] Calling OpenAI API...");
            try (Response response = httpClient.newCall(request).execute()) {
                long apiDuration = System.currentTimeMillis() - apiStart;
                logger.info("[OPENAI] API responded in {}ms — HTTP {}", apiDuration, response.code());

                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                    logger.error("[OPENAI] API ERROR (HTTP {}): {}", response.code(), errorBody);
                    throw new RuntimeException("OpenAI API error (HTTP " + response.code() + "): " + errorBody);
                }

                String responseBody = response.body().string();
                logger.info("[OPENAI] Response body size: {} bytes", responseBody.length());
                LlmResponse llmResponse = parseResponse(responseBody);
                logger.info("[OPENAI] Parsed response: promptTokens={}, completionTokens={}, totalTokens={}, "
                                + "hasContent={}, toolCalls={}",
                        llmResponse.getPromptTokens(), llmResponse.getCompletionTokens(),
                        llmResponse.getTotalTokens(),
                        llmResponse.getContent() != null && !llmResponse.getContent().isEmpty(),
                        llmResponse.hasToolCalls() ? llmResponse.getToolCalls().size() : 0);
                if (llmResponse.hasToolCalls()) {
                    for (LlmResponse.ToolCall tc : llmResponse.getToolCalls()) {
                        logger.info("[OPENAI]   Tool call: {}({})", tc.getFunctionName(), tc.getArguments());
                    }
                }
                return llmResponse;
            }
        } catch (IOException e) {
            logger.error("[OPENAI] Communication failure: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to communicate with OpenAI API: " + e.getMessage(), e);
        }
    }

    private ObjectNode buildRequestBody(List<ChatMessage> messages, List<Map<String, Object>> tools) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", model);
        body.put("temperature", temperature);
        body.put("max_tokens", maxTokens);

        // Build messages array
        ArrayNode messagesArray = body.putArray("messages");
        for (ChatMessage msg : messages) {
            ObjectNode msgNode = messagesArray.addObject();
            msgNode.put("role", msg.getRoleString());

            if (msg.getToolCallId() != null) {
                msgNode.put("tool_call_id", msg.getToolCallId());
            }

            List<ChatMessage.ContentPart> contentParts = msg.getContent();
            if (contentParts.size() == 1 && contentParts.get(0).getType() == ChatMessage.ContentPart.Type.TEXT) {
                // Simple text message
                msgNode.put("content", contentParts.get(0).getText());
            } else {
                // Multi-part message (text + images)
                ArrayNode contentArray = msgNode.putArray("content");
                for (ChatMessage.ContentPart part : contentParts) {
                    ObjectNode partNode = contentArray.addObject();
                    if (part.getType() == ChatMessage.ContentPart.Type.TEXT) {
                        partNode.put("type", "text");
                        partNode.put("text", part.getText());
                    } else if (part.getType() == ChatMessage.ContentPart.Type.IMAGE) {
                        partNode.put("type", "image_url");
                        ObjectNode imageUrl = partNode.putObject("image_url");
                        imageUrl.put("url", "data:image/png;base64," + part.getBase64Image());
                        imageUrl.put("detail", "auto");
                    }
                }
            }
        }

        // Build tools array
        if (tools != null && !tools.isEmpty()) {
            ArrayNode toolsArray = body.putArray("tools");
            for (Map<String, Object> tool : tools) {
                toolsArray.add(objectMapper.valueToTree(tool));
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
            promptTokens = usage.path("prompt_tokens").asInt(0);
            completionTokens = usage.path("completion_tokens").asInt(0);
        }

        // Parse choice
        JsonNode choices = root.get("choices");
        if (choices == null || choices.isEmpty()) {
            return new LlmResponse("", null, promptTokens, completionTokens);
        }

        JsonNode choice = choices.get(0);
        JsonNode message = choice.get("message");
        String content = message.has("content") && !message.get("content").isNull()
                ? message.get("content").asText() : "";

        // Parse tool calls
        List<LlmResponse.ToolCall> toolCalls = new ArrayList<>();
        if (message.has("tool_calls") && !message.get("tool_calls").isNull()) {
            for (JsonNode tc : message.get("tool_calls")) {
                String id = tc.get("id").asText();
                JsonNode function = tc.get("function");
                String funcName = function.get("name").asText();
                String argsStr = function.get("arguments").asText();
                Map<String, Object> args = objectMapper.readValue(argsStr, new TypeReference<>() {});
                toolCalls.add(new LlmResponse.ToolCall(id, funcName, args));
            }
        }

        logger.debug("[OPENAI] Parsed: content_length={}, tool_calls={}, promptTokens={}, completionTokens={}",
                content.length(), toolCalls.size(), promptTokens, completionTokens);

        return new LlmResponse(content, toolCalls, promptTokens, completionTokens);
    }

    @Override
    public String getProviderName() {
        return "openai";
    }

    @Override
    public String getModelName() {
        return model;
    }

    @Override
    public boolean supportsVision() {
        return model.contains("gpt-4") || model.contains("gpt-4o") || model.contains("vision");
    }
}
