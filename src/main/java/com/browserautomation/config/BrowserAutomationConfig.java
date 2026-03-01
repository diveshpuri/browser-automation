package com.browserautomation.config;

import java.util.Optional;

/**
 * Global configuration for the browser automation library.
 * Reads settings from environment variables and system properties.
 */
public class BrowserAutomationConfig {

    private String openAiApiKey;
    private String openAiBaseUrl;
    private String anthropicApiKey;
    private String anthropicBaseUrl;
    private String azureOpenAiKey;
    private String azureOpenAiEndpoint;
    private String azureOpenAiApiVersion;
    private String geminiApiKey;
    private String deepSeekApiKey;
    private String ollamaBaseUrl;
    private String groqApiKey;
    private String mistralApiKey;
    private String awsAccessKeyId;
    private String awsSecretAccessKey;
    private String awsRegion;
    private String defaultLlmProvider;
    private String defaultModel;
    private String loggingLevel;
    private boolean headless;
    private int defaultTimeout;

    private BrowserAutomationConfig() {
        this.openAiApiKey = getEnv("OPENAI_API_KEY", "");
        this.openAiBaseUrl = getEnv("OPENAI_BASE_URL", "https://api.openai.com/v1");
        this.anthropicApiKey = getEnv("ANTHROPIC_API_KEY", "");
        this.anthropicBaseUrl = getEnv("ANTHROPIC_BASE_URL", "https://api.anthropic.com/v1");
        this.azureOpenAiKey = getEnv("AZURE_OPENAI_KEY", "");
        this.azureOpenAiEndpoint = getEnv("AZURE_OPENAI_ENDPOINT", "");
        this.azureOpenAiApiVersion = getEnv("AZURE_OPENAI_API_VERSION", "2024-10-21");
        this.geminiApiKey = getEnv("GEMINI_API_KEY", "");
        this.deepSeekApiKey = getEnv("DEEPSEEK_API_KEY", "");
        this.ollamaBaseUrl = getEnv("OLLAMA_BASE_URL", "http://localhost:11434");
        this.groqApiKey = getEnv("GROQ_API_KEY", "");
        this.mistralApiKey = getEnv("MISTRAL_API_KEY", "");
        this.awsAccessKeyId = getEnv("AWS_ACCESS_KEY_ID", "");
        this.awsSecretAccessKey = getEnv("AWS_SECRET_ACCESS_KEY", "");
        this.awsRegion = getEnv("AWS_REGION", "us-east-1");
        this.defaultLlmProvider = getEnv("BROWSER_AUTOMATION_LLM_PROVIDER", "openai");
        this.defaultModel = getEnv("BROWSER_AUTOMATION_MODEL", "gpt-4o");
        this.loggingLevel = getEnv("BROWSER_AUTOMATION_LOG_LEVEL", "info");
        this.headless = Boolean.parseBoolean(getEnv("BROWSER_AUTOMATION_HEADLESS", "true"));
        this.defaultTimeout = Integer.parseInt(getEnv("BROWSER_AUTOMATION_TIMEOUT", "30000"));
    }

    private static final BrowserAutomationConfig INSTANCE = new BrowserAutomationConfig();

    public static BrowserAutomationConfig getInstance() {
        return INSTANCE;
    }

    private static String getEnv(String key, String defaultValue) {
        String sysProperty = System.getProperty(key);
        if (sysProperty != null && !sysProperty.isEmpty()) {
            return sysProperty;
        }
        String envVar = System.getenv(key);
        if (envVar != null && !envVar.isEmpty()) {
            return envVar;
        }
        return defaultValue;
    }

    public String getOpenAiApiKey() {
        return openAiApiKey;
    }

    public void setOpenAiApiKey(String openAiApiKey) {
        this.openAiApiKey = openAiApiKey;
    }

    public String getOpenAiBaseUrl() {
        return openAiBaseUrl;
    }

    public void setOpenAiBaseUrl(String openAiBaseUrl) {
        this.openAiBaseUrl = openAiBaseUrl;
    }

    public String getAnthropicApiKey() {
        return anthropicApiKey;
    }

    public void setAnthropicApiKey(String anthropicApiKey) {
        this.anthropicApiKey = anthropicApiKey;
    }

    public String getAnthropicBaseUrl() {
        return anthropicBaseUrl;
    }

    public void setAnthropicBaseUrl(String anthropicBaseUrl) {
        this.anthropicBaseUrl = anthropicBaseUrl;
    }

    public String getDefaultLlmProvider() {
        return defaultLlmProvider;
    }

    public void setDefaultLlmProvider(String defaultLlmProvider) {
        this.defaultLlmProvider = defaultLlmProvider;
    }

    public String getDefaultModel() {
        return defaultModel;
    }

    public void setDefaultModel(String defaultModel) {
        this.defaultModel = defaultModel;
    }

    public String getLoggingLevel() {
        return loggingLevel;
    }

    public void setLoggingLevel(String loggingLevel) {
        this.loggingLevel = loggingLevel;
    }

    public boolean isHeadless() {
        return headless;
    }

    public void setHeadless(boolean headless) {
        this.headless = headless;
    }

    public int getDefaultTimeout() {
        return defaultTimeout;
    }

    public void setDefaultTimeout(int defaultTimeout) {
        this.defaultTimeout = defaultTimeout;
    }

    public String getAzureOpenAiKey() {
        return azureOpenAiKey;
    }

    public void setAzureOpenAiKey(String azureOpenAiKey) {
        this.azureOpenAiKey = azureOpenAiKey;
    }

    public String getAzureOpenAiEndpoint() {
        return azureOpenAiEndpoint;
    }

    public void setAzureOpenAiEndpoint(String azureOpenAiEndpoint) {
        this.azureOpenAiEndpoint = azureOpenAiEndpoint;
    }

    public String getAzureOpenAiApiVersion() {
        return azureOpenAiApiVersion;
    }

    public void setAzureOpenAiApiVersion(String azureOpenAiApiVersion) {
        this.azureOpenAiApiVersion = azureOpenAiApiVersion;
    }

    public String getGeminiApiKey() {
        return geminiApiKey;
    }

    public void setGeminiApiKey(String geminiApiKey) {
        this.geminiApiKey = geminiApiKey;
    }

    public String getDeepSeekApiKey() {
        return deepSeekApiKey;
    }

    public void setDeepSeekApiKey(String deepSeekApiKey) {
        this.deepSeekApiKey = deepSeekApiKey;
    }

    public String getOllamaBaseUrl() {
        return ollamaBaseUrl;
    }

    public void setOllamaBaseUrl(String ollamaBaseUrl) {
        this.ollamaBaseUrl = ollamaBaseUrl;
    }

    public String getGroqApiKey() {
        return groqApiKey;
    }

    public void setGroqApiKey(String groqApiKey) {
        this.groqApiKey = groqApiKey;
    }

    public String getMistralApiKey() {
        return mistralApiKey;
    }

    public void setMistralApiKey(String mistralApiKey) {
        this.mistralApiKey = mistralApiKey;
    }

    public String getAwsAccessKeyId() {
        return awsAccessKeyId;
    }

    public void setAwsAccessKeyId(String awsAccessKeyId) {
        this.awsAccessKeyId = awsAccessKeyId;
    }

    public String getAwsSecretAccessKey() {
        return awsSecretAccessKey;
    }

    public void setAwsSecretAccessKey(String awsSecretAccessKey) {
        this.awsSecretAccessKey = awsSecretAccessKey;
    }

    public String getAwsRegion() {
        return awsRegion;
    }

    public void setAwsRegion(String awsRegion) {
        this.awsRegion = awsRegion;
    }
}
