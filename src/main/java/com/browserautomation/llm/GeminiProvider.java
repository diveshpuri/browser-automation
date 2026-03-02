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
 * Google Gemini LLM provider.
 * Uses the Gemini API (generativelanguage.googleapis.com) for chat completions with tool support.
 *
 * <p>Required environment variables:</p>
 * <ul>
 *   <li>{@code GEMINI_API_KEY} - Google Gemini API key</li>
 * </ul>
 *
 * <p>Supported models include gemini-3-flash-preview, gemini-1.5-pro, gemini-1.5-flash, etc.</p>
 */
public class GeminiProvider implements LlmProvider {

    private static final Logger logger = LoggerFactory.getLogger(GeminiProvider.class);
    private static final MediaType JSON_TYPE = MediaType.parse("application/json");
    private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta";

    private final String apiKey;
    private final String model;
    private final double temperature;
    private final int maxTokens;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    /**
     * Create a Gemini provider with minimal configuration.
     *
     * @param apiKey the Gemini API key
     * @param model  the model name (e.g., "gemini-3-flash-preview", "gemini-1.5-pro")
     */
    public GeminiProvider(String apiKey, String model) {
        this(apiKey, model, 0.0, 4096);
    }

    /**
     * Create a Gemini provider with full configuration.
     *
     * @param apiKey      the Gemini API key
     * @param model       the model name
     * @param temperature the sampling temperature
     * @param maxTokens   the maximum number of tokens to generate
     */
    public GeminiProvider(String apiKey, String model, double temperature, int maxTokens) {
        this.apiKey = apiKey;
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
    public static GeminiProvider fromConfig() {
        BrowserAutomationConfig config = BrowserAutomationConfig.getInstance();
        return new GeminiProvider(
                config.getGeminiApiKey(),
                config.getDefaultModel()
        );
    }

    @Override
    public LlmResponse chatCompletion(List<ChatMessage> messages, List<Map<String, Object>> tools) {
        try {
            // Log request details
            int systemMsgCount = 0, userMsgCount = 0, assistantMsgCount = 0, toolMsgCount = 0;
            int totalTextChars = 0;
            boolean hasImages = false;
            for (ChatMessage msg : messages) {
                switch (msg.getRole()) {
                    case SYSTEM -> systemMsgCount++;
                    case USER -> userMsgCount++;
                    case ASSISTANT -> assistantMsgCount++;
                    case TOOL -> toolMsgCount++;
                }
                for (ChatMessage.ContentPart part : msg.getContent()) {
                    if (part.getType() == ChatMessage.ContentPart.Type.TEXT && part.getText() != null) {
                        totalTextChars += part.getText().length();
                    } else if (part.getType() == ChatMessage.ContentPart.Type.IMAGE) {
                        hasImages = true;
                    }
                }
            }
            logger.info("[GEMINI] Request: model={}, messages={} (system={}, user={}, assistant={}, tool={}), "
                            + "tools={}, totalTextChars={}, hasImages={}",
                    model, messages.size(), systemMsgCount, userMsgCount, assistantMsgCount, toolMsgCount,
                    tools != null ? tools.size() : 0, totalTextChars, hasImages);

            ObjectNode requestBody = buildRequestBody(messages, tools);
            String jsonBody = objectMapper.writeValueAsString(requestBody);
            logger.info("[GEMINI] Request body size: {} bytes", jsonBody.length());

            String url = BASE_URL + "/models/" + model + ":generateContent?key=" + apiKey;

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(jsonBody, JSON_TYPE))
                    .build();

            long apiStart = System.currentTimeMillis();
            logger.info("[GEMINI] Calling Gemini API...");
            try (Response response = httpClient.newCall(request).execute()) {
                long apiDuration = System.currentTimeMillis() - apiStart;
                logger.info("[GEMINI] API responded in {}ms — HTTP {}", apiDuration, response.code());

                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                    logger.error("[GEMINI] API ERROR (HTTP {}): {}", response.code(), errorBody);
                    throw new RuntimeException("Gemini API error (HTTP " + response.code() + "): " + errorBody);
                }

                String responseBody = response.body().string();
                logger.info("[GEMINI] Response body size: {} bytes", responseBody.length());
                LlmResponse llmResponse = parseResponse(responseBody);
                logger.info("[GEMINI] Parsed response: promptTokens={}, completionTokens={}, totalTokens={}, "
                                + "hasContent={}, toolCalls={}",
                        llmResponse.getPromptTokens(), llmResponse.getCompletionTokens(),
                        llmResponse.getTotalTokens(),
                        llmResponse.getContent() != null && !llmResponse.getContent().isEmpty(),
                        llmResponse.hasToolCalls() ? llmResponse.getToolCalls().size() : 0);
                if (llmResponse.hasToolCalls()) {
                    for (LlmResponse.ToolCall tc : llmResponse.getToolCalls()) {
                        logger.info("[GEMINI]   Tool call: {}({})", tc.getFunctionName(), tc.getArguments());
                    }
                }
                return llmResponse;
            }
        } catch (IOException e) {
            logger.error("[GEMINI] Communication failure: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to communicate with Gemini API: " + e.getMessage(), e);
        }
    }

    private ObjectNode buildRequestBody(List<ChatMessage> messages, List<Map<String, Object>> tools) {
        ObjectNode body = objectMapper.createObjectNode();

        // Generation config
        ObjectNode generationConfig = body.putObject("generationConfig");
        generationConfig.put("temperature", temperature);
        generationConfig.put("maxOutputTokens", maxTokens);

        // Extract system instruction
        String systemInstruction = null;
        List<ChatMessage> conversationMessages = new ArrayList<>();
        for (ChatMessage msg : messages) {
            if (msg.getRole() == ChatMessage.Role.SYSTEM) {
                systemInstruction = msg.getText();
            } else {
                conversationMessages.add(msg);
            }
        }

        if (systemInstruction != null) {
            ObjectNode systemNode = body.putObject("systemInstruction");
            ArrayNode systemParts = systemNode.putArray("parts");
            systemParts.addObject().put("text", systemInstruction);
        }

        // Build contents array
        ArrayNode contents = body.putArray("contents");
        for (ChatMessage msg : conversationMessages) {
            ObjectNode contentNode = contents.addObject();
            String role = mapRole(msg.getRole());
            contentNode.put("role", role);

            ArrayNode parts = contentNode.putArray("parts");
            for (ChatMessage.ContentPart part : msg.getContent()) {
                if (part.getType() == ChatMessage.ContentPart.Type.TEXT) {
                    parts.addObject().put("text", part.getText());
                } else if (part.getType() == ChatMessage.ContentPart.Type.IMAGE) {
                    ObjectNode imagePart = parts.addObject();
                    ObjectNode inlineData = imagePart.putObject("inlineData");
                    inlineData.put("mimeType", "image/png");
                    inlineData.put("data", part.getBase64Image());
                }
            }

            // Handle tool call results
            if (msg.getRole() == ChatMessage.Role.TOOL && msg.getToolCallId() != null) {
                ObjectNode functionResponse = parts.addObject().putObject("functionResponse");
                functionResponse.put("name", msg.getToolCallId());
                ObjectNode responseObj = functionResponse.putObject("response");
                responseObj.put("result", msg.getText());
            }
        }

        // Build tools (function declarations)
        if (tools != null && !tools.isEmpty()) {
            ArrayNode toolsArray = body.putArray("tools");
            ObjectNode toolObj = toolsArray.addObject();
            ArrayNode functionDeclarations = toolObj.putArray("functionDeclarations");

            for (Map<String, Object> tool : tools) {
                @SuppressWarnings("unchecked")
                Map<String, Object> function = (Map<String, Object>) tool.get("function");
                if (function != null) {
                    ObjectNode funcNode = functionDeclarations.addObject();
                    funcNode.put("name", (String) function.get("name"));
                    funcNode.put("description", (String) function.get("description"));

                    Object params = function.get("parameters");
                    if (params != null) {
                        funcNode.set("parameters", objectMapper.valueToTree(params));
                    }
                }
            }
        }

        return body;
    }

    private String mapRole(ChatMessage.Role role) {
        return switch (role) {
            case USER, TOOL -> "user";
            case ASSISTANT -> "model";
            default -> "user";
        };
    }

    private LlmResponse parseResponse(String responseBody) throws IOException {
        JsonNode root = objectMapper.readTree(responseBody);

        // Parse usage metadata
        int promptTokens = 0;
        int completionTokens = 0;
        JsonNode usageMetadata = root.get("usageMetadata");
        if (usageMetadata != null) {
            promptTokens = usageMetadata.path("promptTokenCount").asInt(0);
            completionTokens = usageMetadata.path("candidatesTokenCount").asInt(0);
        }

        // Parse candidates
        JsonNode candidates = root.get("candidates");
        if (candidates == null || candidates.isEmpty()) {
            return new LlmResponse("", null, promptTokens, completionTokens);
        }

        JsonNode candidate = candidates.get(0);
        JsonNode content = candidate.get("content");
        if (content == null) {
            return new LlmResponse("", null, promptTokens, completionTokens);
        }

        StringBuilder textContent = new StringBuilder();
        List<LlmResponse.ToolCall> toolCalls = new ArrayList<>();

        JsonNode parts = content.get("parts");
        if (parts != null && parts.isArray()) {
            for (JsonNode part : parts) {
                if (part.has("text")) {
                    textContent.append(part.get("text").asText());
                } else if (part.has("functionCall")) {
                    JsonNode functionCall = part.get("functionCall");
                    String name = functionCall.get("name").asText();
                    JsonNode argsNode = functionCall.get("args");
                    Map<String, Object> args = argsNode != null
                            ? objectMapper.convertValue(argsNode, new TypeReference<>() {})
                            : Map.of();
                    // Gemini doesn't provide call IDs, generate one
                    String callId = "call_" + name + "_" + System.nanoTime();
                    toolCalls.add(new LlmResponse.ToolCall(callId, name, args));
                }
            }
        }

        logger.debug("[GEMINI] Parsed: content_length={}, tool_calls={}, promptTokens={}, completionTokens={}",
                textContent.length(), toolCalls.size(), promptTokens, completionTokens);

        return new LlmResponse(textContent.toString(), toolCalls, promptTokens, completionTokens);
    }

    @Override
    public String getProviderName() {
        return "gemini";
    }

    @Override
    public String getModelName() {
        return model;
    }

    @Override
    public boolean supportsVision() {
        return model.contains("gemini-1.5") || model.contains("gemini-2")
                || model.contains("gemini-3") || model.contains("gemini-pro-vision");
    }
}
