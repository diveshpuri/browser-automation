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

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * AWS Bedrock LLM provider.
 * Supports invoking foundation models via the AWS Bedrock Runtime API.
 *
 * <p>Required environment variables:</p>
 * <ul>
 *   <li>{@code AWS_ACCESS_KEY_ID} - AWS access key</li>
 *   <li>{@code AWS_SECRET_ACCESS_KEY} - AWS secret key</li>
 *   <li>{@code AWS_REGION} - AWS region (default: us-east-1)</li>
 * </ul>
 *
 * <p>Supported models:</p>
 * <ul>
 *   <li>anthropic.claude-3-5-sonnet-20241022-v2:0</li>
 *   <li>anthropic.claude-3-haiku-20240307-v1:0</li>
 *   <li>amazon.titan-text-express-v1</li>
 *   <li>meta.llama3-1-70b-instruct-v1:0</li>
 *   <li>mistral.mixtral-8x7b-instruct-v0:1</li>
 * </ul>
 */
public class AwsBedrockProvider implements LlmProvider {

    private static final Logger logger = LoggerFactory.getLogger(AwsBedrockProvider.class);
    private static final MediaType JSON_TYPE = MediaType.parse("application/json");
    private static final String SERVICE = "bedrock";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");

    private final String accessKeyId;
    private final String secretAccessKey;
    private final String region;
    private final String model;
    private final double temperature;
    private final int maxTokens;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    /**
     * Create an AWS Bedrock provider.
     *
     * @param accessKeyId     AWS access key ID
     * @param secretAccessKey AWS secret access key
     * @param region          AWS region
     * @param model           the Bedrock model ID
     */
    public AwsBedrockProvider(String accessKeyId, String secretAccessKey, String region, String model) {
        this(accessKeyId, secretAccessKey, region, model, 0.0, 4096);
    }

    /**
     * Create an AWS Bedrock provider with custom settings.
     */
    public AwsBedrockProvider(String accessKeyId, String secretAccessKey, String region,
                              String model, double temperature, int maxTokens) {
        this.accessKeyId = accessKeyId;
        this.secretAccessKey = secretAccessKey;
        this.region = region;
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
    public static AwsBedrockProvider fromConfig() {
        BrowserAutomationConfig config = BrowserAutomationConfig.getInstance();
        return new AwsBedrockProvider(
                config.getAwsAccessKeyId(),
                config.getAwsSecretAccessKey(),
                config.getAwsRegion(),
                "anthropic.claude-3-5-sonnet-20241022-v2:0"
        );
    }

    @Override
    public LlmResponse chatCompletion(List<ChatMessage> messages, List<Map<String, Object>> tools) {
        try {
            // Build the request using the Converse API
            String endpoint = String.format(
                    "https://bedrock-runtime.%s.amazonaws.com/model/%s/converse", region, model);

            ObjectNode requestBody = buildConverseRequest(messages, tools);
            String jsonBody = objectMapper.writeValueAsString(requestBody);

            logger.debug("Sending request to AWS Bedrock: model={}, region={}", model, region);

            // Sign the request with AWS Signature V4
            ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
            String dateStamp = now.format(DATE_FORMAT);
            String amzDate = now.format(DATETIME_FORMAT);

            URI uri = URI.create(endpoint);
            String canonicalUri = uri.getPath();
            String host = uri.getHost();

            String payloadHash = sha256Hex(jsonBody);

            String canonicalHeaders = "content-type:application/json\n"
                    + "host:" + host + "\n"
                    + "x-amz-date:" + amzDate + "\n";
            String signedHeaders = "content-type;host;x-amz-date";

            String canonicalRequest = "POST\n" + canonicalUri + "\n\n"
                    + canonicalHeaders + "\n" + signedHeaders + "\n" + payloadHash;

            String credentialScope = dateStamp + "/" + region + "/bedrock/aws4_request";
            String stringToSign = "AWS4-HMAC-SHA256\n" + amzDate + "\n"
                    + credentialScope + "\n" + sha256Hex(canonicalRequest);

            byte[] signingKey = getSignatureKey(secretAccessKey, dateStamp, region, "bedrock");
            String signature = hmacSha256Hex(signingKey, stringToSign);

            String authorizationHeader = "AWS4-HMAC-SHA256 Credential=" + accessKeyId + "/" + credentialScope
                    + ", SignedHeaders=" + signedHeaders + ", Signature=" + signature;

            Request request = new Request.Builder()
                    .url(endpoint)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("X-Amz-Date", amzDate)
                    .addHeader("Authorization", authorizationHeader)
                    .post(RequestBody.create(jsonBody, JSON_TYPE))
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                    throw new RuntimeException("AWS Bedrock API error (HTTP " + response.code() + "): " + errorBody);
                }

                String responseBody = response.body().string();
                return parseConverseResponse(responseBody);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to communicate with AWS Bedrock: " + e.getMessage(), e);
        }
    }

    private ObjectNode buildConverseRequest(List<ChatMessage> messages, List<Map<String, Object>> tools) {
        ObjectNode body = objectMapper.createObjectNode();

        // Build messages array
        ArrayNode messagesArray = body.putArray("messages");
        String systemContent = null;

        for (ChatMessage msg : messages) {
            if (msg.getRole() == ChatMessage.Role.SYSTEM) {
                systemContent = msg.getText();
                continue;
            }

            ObjectNode msgNode = messagesArray.addObject();
            msgNode.put("role", msg.getRole() == ChatMessage.Role.ASSISTANT ? "assistant" : "user");

            ArrayNode contentArray = msgNode.putArray("content");
            for (ChatMessage.ContentPart part : msg.getContent()) {
                ObjectNode partNode = contentArray.addObject();
                if (part.getType() == ChatMessage.ContentPart.Type.TEXT) {
                    partNode.put("text", part.getText());
                } else if (part.getType() == ChatMessage.ContentPart.Type.IMAGE) {
                    ObjectNode imageNode = partNode.putObject("image");
                    imageNode.put("format", "png");
                    ObjectNode sourceNode = imageNode.putObject("source");
                    sourceNode.put("bytes", part.getBase64Image());
                }
            }
        }

        // Set system message
        if (systemContent != null) {
            ArrayNode systemArray = body.putArray("system");
            ObjectNode systemNode = systemArray.addObject();
            systemNode.put("text", systemContent);
        }

        // Inference config
        ObjectNode inferenceConfig = body.putObject("inferenceConfig");
        inferenceConfig.put("maxTokens", maxTokens);
        inferenceConfig.put("temperature", (float) temperature);

        // Tools
        if (tools != null && !tools.isEmpty()) {
            ObjectNode toolConfig = body.putObject("toolConfig");
            ArrayNode toolsArray = toolConfig.putArray("tools");
            for (Map<String, Object> tool : tools) {
                toolsArray.add(objectMapper.valueToTree(tool));
            }
        }

        return body;
    }

    private LlmResponse parseConverseResponse(String responseBody) throws IOException {
        JsonNode root = objectMapper.readTree(responseBody);

        int promptTokens = 0;
        int completionTokens = 0;
        JsonNode usage = root.get("usage");
        if (usage != null) {
            promptTokens = usage.path("inputTokens").asInt(0);
            completionTokens = usage.path("outputTokens").asInt(0);
        }

        JsonNode output = root.get("output");
        if (output == null) {
            return new LlmResponse("", null, promptTokens, completionTokens);
        }

        JsonNode message = output.get("message");
        if (message == null) {
            return new LlmResponse("", null, promptTokens, completionTokens);
        }

        StringBuilder content = new StringBuilder();
        List<LlmResponse.ToolCall> toolCalls = new ArrayList<>();

        JsonNode contentArray = message.get("content");
        if (contentArray != null) {
            for (JsonNode block : contentArray) {
                if (block.has("text")) {
                    content.append(block.get("text").asText());
                } else if (block.has("toolUse")) {
                    JsonNode toolUse = block.get("toolUse");
                    String id = toolUse.get("toolUseId").asText();
                    String name = toolUse.get("name").asText();
                    Map<String, Object> args = objectMapper.readValue(
                            toolUse.get("input").toString(), new TypeReference<>() {});
                    toolCalls.add(new LlmResponse.ToolCall(id, name, args));
                }
            }
        }

        return new LlmResponse(content.toString(), toolCalls, promptTokens, completionTokens);
    }

    // AWS Signature V4 helpers

    private static String sha256Hex(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    private static byte[] hmacSha256(byte[] key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException("HmacSHA256 not available", e);
        }
    }

    private static String hmacSha256Hex(byte[] key, String data) {
        return bytesToHex(hmacSha256(key, data));
    }

    private static byte[] getSignatureKey(String key, String dateStamp, String regionName, String serviceName) {
        byte[] kDate = hmacSha256(("AWS4" + key).getBytes(StandardCharsets.UTF_8), dateStamp);
        byte[] kRegion = hmacSha256(kDate, regionName);
        byte[] kService = hmacSha256(kRegion, serviceName);
        return hmacSha256(kService, "aws4_request");
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    @Override
    public String getProviderName() {
        return "aws-bedrock";
    }

    @Override
    public String getModelName() {
        return model;
    }

    @Override
    public boolean supportsVision() {
        return model.contains("claude-3") || model.contains("claude-3-5");
    }
}
