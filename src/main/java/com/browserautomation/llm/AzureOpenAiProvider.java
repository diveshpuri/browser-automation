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
 * Azure OpenAI LLM provider.
 * Uses Azure's OpenAI Service endpoints with API key authentication.
 *
 * <p>Required environment variables:</p>
 * <ul>
 *   <li>{@code AZURE_OPENAI_ENDPOINT} - Azure OpenAI endpoint URL (e.g., https://your-resource.openai.azure.com/)</li>
 *   <li>{@code AZURE_OPENAI_KEY} - Azure OpenAI API key</li>
 * </ul>
 */
public class AzureOpenAiProvider implements LlmProvider {

    private static final Logger logger = LoggerFactory.getLogger(AzureOpenAiProvider.class);
    private static final MediaType JSON_TYPE = MediaType.parse("application/json");

    private final String apiKey;
    private final String endpoint;
    private final String model;
    private final String apiVersion;
    private final double temperature;
    private final int maxTokens;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    /**
     * Create an Azure OpenAI provider with minimal configuration.
     *
     * @param apiKey   the Azure OpenAI API key
     * @param endpoint the Azure OpenAI endpoint URL
     * @param model    the deployment name (e.g., "gpt-4o")
     */
    public AzureOpenAiProvider(String apiKey, String endpoint, String model) {
        this(apiKey, endpoint, model, "2024-10-21", 0.0, 4096);
    }

    /**
     * Create an Azure OpenAI provider with full configuration.
     *
     * @param apiKey      the Azure OpenAI API key
     * @param endpoint    the Azure OpenAI endpoint URL
     * @param model       the deployment name
     * @param apiVersion  the API version (e.g., "2024-10-21")
     * @param temperature the sampling temperature
     * @param maxTokens   the maximum number of tokens to generate
     */
    public AzureOpenAiProvider(String apiKey, String endpoint, String model,
                               String apiVersion, double temperature, int maxTokens) {
        this.apiKey = apiKey;
        this.endpoint = endpoint.endsWith("/") ? endpoint.substring(0, endpoint.length() - 1) : endpoint;
        this.model = model;
        this.apiVersion = apiVersion;
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
    public static AzureOpenAiProvider fromConfig() {
        BrowserAutomationConfig config = BrowserAutomationConfig.getInstance();
        return new AzureOpenAiProvider(
                config.getAzureOpenAiKey(),
                config.getAzureOpenAiEndpoint(),
                config.getDefaultModel()
        );
    }

    @Override
    public LlmResponse chatCompletion(List<ChatMessage> messages, List<Map<String, Object>> tools) {
        try {
            ObjectNode requestBody = buildRequestBody(messages, tools);
            String jsonBody = objectMapper.writeValueAsString(requestBody);

            String url = endpoint + "/openai/deployments/" + model + "/chat/completions?api-version=" + apiVersion;
            logger.debug("Sending request to Azure OpenAI API: deployment={}", model);

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("api-key", apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(jsonBody, JSON_TYPE))
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                    throw new RuntimeException("Azure OpenAI API error (HTTP " + response.code() + "): " + errorBody);
                }

                String responseBody = response.body().string();
                return parseResponse(responseBody);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to communicate with Azure OpenAI API: " + e.getMessage(), e);
        }
    }

    private ObjectNode buildRequestBody(List<ChatMessage> messages, List<Map<String, Object>> tools) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("temperature", temperature);
        body.put("max_tokens", maxTokens);

        // Build messages array (same format as OpenAI)
        ArrayNode messagesArray = body.putArray("messages");
        for (ChatMessage msg : messages) {
            ObjectNode msgNode = messagesArray.addObject();
            msgNode.put("role", msg.getRoleString());

            if (msg.getToolCallId() != null) {
                msgNode.put("tool_call_id", msg.getToolCallId());
            }

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
                        partNode.put("type", "image_url");
                        ObjectNode imageUrl = partNode.putObject("image_url");
                        imageUrl.put("url", "data:image/png;base64," + part.getBase64Image());
                        imageUrl.put("detail", "auto");
                    }
                }
            }
        }

        // Build tools array (same format as OpenAI)
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

        int promptTokens = 0;
        int completionTokens = 0;
        JsonNode usage = root.get("usage");
        if (usage != null) {
            promptTokens = usage.path("prompt_tokens").asInt(0);
            completionTokens = usage.path("completion_tokens").asInt(0);
        }

        JsonNode choices = root.get("choices");
        if (choices == null || choices.isEmpty()) {
            return new LlmResponse("", null, promptTokens, completionTokens);
        }

        JsonNode choice = choices.get(0);
        JsonNode message = choice.get("message");
        String content = message.has("content") && !message.get("content").isNull()
                ? message.get("content").asText() : "";

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

        logger.debug("Azure OpenAI response: content_length={}, tool_calls={}, tokens={}",
                content.length(), toolCalls.size(), promptTokens + completionTokens);

        return new LlmResponse(content, toolCalls, promptTokens, completionTokens);
    }

    @Override
    public String getProviderName() {
        return "azure-openai";
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
