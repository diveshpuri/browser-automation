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
 * Ollama LLM provider for running local models.
 * Connects to a local Ollama server using its OpenAI-compatible chat API.
 *
 * <p>Setup:</p>
 * <ol>
 *   <li>Install Ollama from <a href="https://ollama.ai">ollama.ai</a></li>
 *   <li>Pull a model: {@code ollama pull qwen2.5}</li>
 *   <li>Ensure Ollama is running: {@code ollama serve}</li>
 * </ol>
 *
 * <p>Optional environment variables:</p>
 * <ul>
 *   <li>{@code OLLAMA_BASE_URL} - Ollama server URL (default: http://localhost:11434)</li>
 * </ul>
 *
 * <p>Pick a model that supports tool-calling for best results (e.g., qwen2.5, llama3.1).</p>
 */
public class OllamaProvider implements LlmProvider {

    private static final Logger logger = LoggerFactory.getLogger(OllamaProvider.class);
    private static final MediaType JSON_TYPE = MediaType.parse("application/json");
    private static final String DEFAULT_BASE_URL = "http://localhost:11434";

    private final String baseUrl;
    private final String model;
    private final double temperature;
    private final int numCtx;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    /**
     * Create an Ollama provider with minimal configuration.
     *
     * @param model the model name (e.g., "qwen2.5", "llama3.1", "mistral")
     */
    public OllamaProvider(String model) {
        this(DEFAULT_BASE_URL, model, 0.0, 32000);
    }

    /**
     * Create an Ollama provider with custom base URL.
     *
     * @param baseUrl the Ollama server URL (e.g., "http://localhost:11434")
     * @param model   the model name
     */
    public OllamaProvider(String baseUrl, String model) {
        this(baseUrl, model, 0.0, 32000);
    }

    /**
     * Create an Ollama provider with full configuration.
     *
     * @param baseUrl     the Ollama server URL
     * @param model       the model name
     * @param temperature the sampling temperature
     * @param numCtx      the context window size
     */
    public OllamaProvider(String baseUrl, String model, double temperature, int numCtx) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.model = model;
        this.temperature = temperature;
        this.numCtx = numCtx;
        this.objectMapper = new ObjectMapper();
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(300, TimeUnit.SECONDS) // Longer timeout for local inference
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    /**
     * Create from environment configuration.
     */
    public static OllamaProvider fromConfig() {
        BrowserAutomationConfig config = BrowserAutomationConfig.getInstance();
        return new OllamaProvider(
                config.getOllamaBaseUrl(),
                config.getDefaultModel()
        );
    }

    @Override
    public LlmResponse chatCompletion(List<ChatMessage> messages, List<Map<String, Object>> tools) {
        try {
            ObjectNode requestBody = buildRequestBody(messages, tools);
            String jsonBody = objectMapper.writeValueAsString(requestBody);

            String url = baseUrl + "/api/chat";
            logger.debug("Sending request to Ollama: model={}", model);

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(jsonBody, JSON_TYPE))
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                    throw new RuntimeException("Ollama API error (HTTP " + response.code() + "): " + errorBody);
                }

                String responseBody = response.body().string();
                return parseResponse(responseBody);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to communicate with Ollama (is it running?): " + e.getMessage(), e);
        }
    }

    private ObjectNode buildRequestBody(List<ChatMessage> messages, List<Map<String, Object>> tools) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", model);
        body.put("stream", false);

        // Options
        ObjectNode options = body.putObject("options");
        options.put("temperature", temperature);
        options.put("num_ctx", numCtx);

        // Build messages array (OpenAI-compatible format)
        ArrayNode messagesArray = body.putArray("messages");
        for (ChatMessage msg : messages) {
            ObjectNode msgNode = messagesArray.addObject();
            msgNode.put("role", msg.getRoleString());

            // For multi-part messages, combine text and images
            List<ChatMessage.ContentPart> contentParts = msg.getContent();
            StringBuilder textBuilder = new StringBuilder();
            ArrayNode imagesArray = null;

            for (ChatMessage.ContentPart part : contentParts) {
                if (part.getType() == ChatMessage.ContentPart.Type.TEXT) {
                    textBuilder.append(part.getText());
                } else if (part.getType() == ChatMessage.ContentPart.Type.IMAGE) {
                    if (imagesArray == null) {
                        imagesArray = msgNode.putArray("images");
                    }
                    imagesArray.add(part.getBase64Image());
                }
            }

            msgNode.put("content", textBuilder.toString());
        }

        // Build tools array (OpenAI-compatible format)
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
        if (root.has("prompt_eval_count")) {
            promptTokens = root.get("prompt_eval_count").asInt(0);
        }
        if (root.has("eval_count")) {
            completionTokens = root.get("eval_count").asInt(0);
        }

        // Parse message
        JsonNode message = root.get("message");
        if (message == null) {
            return new LlmResponse("", null, promptTokens, completionTokens);
        }

        String content = message.has("content") ? message.get("content").asText("") : "";

        // Parse tool calls
        List<LlmResponse.ToolCall> toolCalls = new ArrayList<>();
        if (message.has("tool_calls") && message.get("tool_calls").isArray()) {
            for (JsonNode tc : message.get("tool_calls")) {
                JsonNode function = tc.get("function");
                if (function != null) {
                    String funcName = function.get("name").asText();
                    JsonNode argsNode = function.get("arguments");
                    Map<String, Object> args = argsNode != null
                            ? objectMapper.convertValue(argsNode, new TypeReference<>() {})
                            : Map.of();
                    String callId = "call_" + funcName + "_" + System.nanoTime();
                    toolCalls.add(new LlmResponse.ToolCall(callId, funcName, args));
                }
            }
        }

        logger.debug("Ollama response: content_length={}, tool_calls={}, tokens={}",
                content.length(), toolCalls.size(), promptTokens + completionTokens);

        return new LlmResponse(content, toolCalls, promptTokens, completionTokens);
    }

    @Override
    public String getProviderName() {
        return "ollama";
    }

    @Override
    public String getModelName() {
        return model;
    }

    @Override
    public boolean supportsVision() {
        // Some Ollama models support vision (e.g., llava, bakllava)
        return model.contains("llava") || model.contains("bakllava") || model.contains("moondream");
    }
}
