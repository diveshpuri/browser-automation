package com.browserautomation;

import com.browserautomation.action.ActionRegistry;
import com.browserautomation.agent.Agent;
import com.browserautomation.agent.AgentConfig;
import com.browserautomation.agent.AgentResult;
import com.browserautomation.browser.BrowserEngineType;
import com.browserautomation.browser.BrowserProfile;
import com.browserautomation.browser.BrowserSession;
import com.browserautomation.llm.AnthropicProvider;
import com.browserautomation.llm.AwsBedrockProvider;
import com.browserautomation.llm.AzureOpenAiProvider;
import com.browserautomation.llm.DeepSeekProvider;
import com.browserautomation.llm.GeminiProvider;
import com.browserautomation.llm.GroqProvider;
import com.browserautomation.llm.LlmProvider;
import com.browserautomation.llm.MistralProvider;
import com.browserautomation.llm.OllamaProvider;
import com.browserautomation.llm.OpenAiProvider;
import com.browserautomation.config.BrowserAutomationConfig;

/**
 * Main entry point and facade for the browser automation library.
 *
 * <p>Provides a simple, fluent API for creating and running browser automation agents.
 * Inspired by the Python browser-use library.</p>
 *
 * <h2>Quick Start</h2>
 * <pre>{@code
 * AgentResult result = BrowserAutomation.agent()
 *     .task("Compare the price of gpt-4o and DeepSeek-V3")
 *     .openAi("gpt-4o")
 *     .run();
 * System.out.println(result.getResult());
 * }</pre>
 *
 * <h2>With Custom Configuration</h2>
 * <pre>{@code
 * AgentResult result = BrowserAutomation.agent()
 *     .task("Find the top 5 trending repositories on GitHub")
 *     .openAi("gpt-4o", "your-api-key")
 *     .browserProfile(new BrowserProfile().headless(false))
 *     .config(new AgentConfig().maxSteps(20).useVision(true))
 *     .run();
 * }</pre>
 *
 * <h2>With Selenium Engine</h2>
 * <pre>{@code
 * AgentResult result = BrowserAutomation.agent()
 *     .task("Search for browser automation on Google")
 *     .useSelenium()
 *     .gemini("gemini-3-flash-preview")
 *     .run();
 * }</pre>
 *
 * @see Agent
 * @see AgentConfig
 * @see BrowserProfile
 */
public class BrowserAutomation {

    /**
     * Library version.
     */
    public static final String VERSION = "0.1.0";

    private BrowserAutomation() {
        // Utility class
    }

    /**
     * Create a new agent builder for fluent configuration.
     *
     * @return a new AgentBuilder
     */
    public static AgentBuilder agent() {
        return new AgentBuilder();
    }

    /**
     * Create a new browser session for manual browser control.
     *
     * @return a new BrowserSession with default settings
     */
    public static BrowserSession createBrowserSession() {
        return new BrowserSession();
    }

    /**
     * Create a new browser session with a custom profile.
     *
     * @param profile the browser configuration profile
     * @return a new BrowserSession
     */
    public static BrowserSession createBrowserSession(BrowserProfile profile) {
        return new BrowserSession(profile);
    }

    /**
     * Fluent builder for creating and running agents with minimal boilerplate.
     */
    public static class AgentBuilder {
        private String task;
        private LlmProvider llmProvider;
        private BrowserProfile browserProfile;
        private AgentConfig agentConfig;
        private ActionRegistry actionRegistry;
        private BrowserSession browserSession;

        /**
         * Set the task for the agent.
         */
        public AgentBuilder task(String task) {
            this.task = task;
            return this;
        }

        /**
         * Use OpenAI as the LLM provider with the default API key from environment.
         *
         * @param model the model name (e.g., "gpt-4o", "gpt-4o-mini")
         */
        public AgentBuilder openAi(String model) {
            this.llmProvider = new OpenAiProvider(
                    BrowserAutomationConfig.getInstance().getOpenAiApiKey(), model);
            return this;
        }

        /**
         * Use OpenAI as the LLM provider with an explicit API key.
         *
         * @param model  the model name
         * @param apiKey the OpenAI API key
         */
        public AgentBuilder openAi(String model, String apiKey) {
            this.llmProvider = new OpenAiProvider(apiKey, model);
            return this;
        }

        /**
         * Use Anthropic as the LLM provider with the default API key from environment.
         *
         * @param model the model name (e.g., "claude-sonnet-4-20250514")
         */
        public AgentBuilder anthropic(String model) {
            this.llmProvider = new AnthropicProvider(
                    BrowserAutomationConfig.getInstance().getAnthropicApiKey(), model);
            return this;
        }

        /**
         * Use Anthropic as the LLM provider with an explicit API key.
         *
         * @param model  the model name
         * @param apiKey the Anthropic API key
         */
        public AgentBuilder anthropic(String model, String apiKey) {
            this.llmProvider = new AnthropicProvider(apiKey, model);
            return this;
        }

        /**
         * Use Azure OpenAI as the LLM provider.
         *
         * @param model    the deployment name (e.g., "gpt-4o")
         * @param apiKey   the Azure OpenAI API key
         * @param endpoint the Azure OpenAI endpoint URL
         */
        public AgentBuilder azureOpenAi(String model, String apiKey, String endpoint) {
            this.llmProvider = new AzureOpenAiProvider(apiKey, endpoint, model);
            return this;
        }

        /**
         * Use Azure OpenAI with environment variables (AZURE_OPENAI_KEY, AZURE_OPENAI_ENDPOINT).
         *
         * @param model the deployment name
         */
        public AgentBuilder azureOpenAi(String model) {
            BrowserAutomationConfig config = BrowserAutomationConfig.getInstance();
            this.llmProvider = new AzureOpenAiProvider(
                    config.getAzureOpenAiKey(), config.getAzureOpenAiEndpoint(), model);
            return this;
        }

        /**
         * Use Google Gemini as the LLM provider.
         *
         * @param model  the model name (e.g., "gemini-3-flash-preview", "gemini-1.5-pro")
         * @param apiKey the Gemini API key
         */
        public AgentBuilder gemini(String model, String apiKey) {
            this.llmProvider = new GeminiProvider(apiKey, model);
            return this;
        }

        /**
         * Use Google Gemini with the GEMINI_API_KEY environment variable.
         *
         * @param model the model name
         */
        public AgentBuilder gemini(String model) {
            this.llmProvider = new GeminiProvider(
                    BrowserAutomationConfig.getInstance().getGeminiApiKey(), model);
            return this;
        }

        /**
         * Use DeepSeek as the LLM provider.
         * Supports DeepSeek-V3 ("deepseek-chat") and DeepSeek-R1 ("deepseek-reasoner").
         * Note: DeepSeek does not support vision.
         *
         * @param model  the model name
         * @param apiKey the DeepSeek API key
         */
        public AgentBuilder deepSeek(String model, String apiKey) {
            this.llmProvider = new DeepSeekProvider(apiKey, model);
            return this;
        }

        /**
         * Use DeepSeek with the DEEPSEEK_API_KEY environment variable.
         *
         * @param model the model name
         */
        public AgentBuilder deepSeek(String model) {
            this.llmProvider = new DeepSeekProvider(
                    BrowserAutomationConfig.getInstance().getDeepSeekApiKey(), model);
            return this;
        }

        /**
         * Use Ollama for local model inference.
         * Requires Ollama to be running locally.
         *
         * @param model the model name (e.g., "qwen2.5", "llama3.1", "mistral")
         */
        public AgentBuilder ollama(String model) {
            this.llmProvider = new OllamaProvider(model);
            return this;
        }

        /**
         * Use Ollama with a custom server URL.
         *
         * @param model   the model name
         * @param baseUrl the Ollama server URL (e.g., "http://localhost:11434")
         */
        public AgentBuilder ollama(String model, String baseUrl) {
            this.llmProvider = new OllamaProvider(baseUrl, model);
            return this;
        }

        /**
         * Use Groq for ultra-fast inference.
         * Supports LLaMA, Mixtral, and Gemma models.
         *
         * @param model the model name (e.g., "llama-3.3-70b-versatile")
         */
        public AgentBuilder groq(String model) {
            this.llmProvider = new GroqProvider(
                    BrowserAutomationConfig.getInstance().getGroqApiKey(), model);
            return this;
        }

        /**
         * Use Groq with an explicit API key.
         */
        public AgentBuilder groq(String model, String apiKey) {
            this.llmProvider = new GroqProvider(apiKey, model);
            return this;
        }

        /**
         * Use Mistral AI as the LLM provider.
         *
         * @param model the model name (e.g., "mistral-large-latest")
         */
        public AgentBuilder mistral(String model) {
            this.llmProvider = new MistralProvider(
                    BrowserAutomationConfig.getInstance().getMistralApiKey(), model);
            return this;
        }

        /**
         * Use Mistral AI with an explicit API key.
         */
        public AgentBuilder mistral(String model, String apiKey) {
            this.llmProvider = new MistralProvider(apiKey, model);
            return this;
        }

        /**
         * Use AWS Bedrock as the LLM provider.
         *
         * @param model the Bedrock model ID (e.g., "anthropic.claude-3-5-sonnet-20241022-v2:0")
         */
        public AgentBuilder awsBedrock(String model) {
            BrowserAutomationConfig config = BrowserAutomationConfig.getInstance();
            this.llmProvider = new AwsBedrockProvider(
                    config.getAwsAccessKeyId(), config.getAwsSecretAccessKey(),
                    config.getAwsRegion(), model);
            return this;
        }

        /**
         * Use AWS Bedrock with explicit credentials.
         */
        public AgentBuilder awsBedrock(String model, String accessKeyId,
                                       String secretAccessKey, String region) {
            this.llmProvider = new AwsBedrockProvider(accessKeyId, secretAccessKey, region, model);
            return this;
        }

        /**
         * Use a custom LLM provider.
         */
        public AgentBuilder llmProvider(LlmProvider provider) {
            this.llmProvider = provider;
            return this;
        }

        /**
         * Use Selenium as the browser automation engine.
         * By default, Playwright is used.
         */
        public AgentBuilder useSelenium() {
            if (this.browserProfile == null) {
                this.browserProfile = new BrowserProfile();
            }
            this.browserProfile.useSelenium();
            return this;
        }

        /**
         * Use Playwright as the browser automation engine (this is the default).
         */
        public AgentBuilder usePlaywright() {
            if (this.browserProfile == null) {
                this.browserProfile = new BrowserProfile();
            }
            this.browserProfile.usePlaywright();
            return this;
        }

        /**
         * Set the browser engine type.
         *
         * @param engineType PLAYWRIGHT or SELENIUM
         */
        public AgentBuilder engineType(BrowserEngineType engineType) {
            if (this.browserProfile == null) {
                this.browserProfile = new BrowserProfile();
            }
            this.browserProfile.engineType(engineType);
            return this;
        }

        /**
         * Set the browser profile.
         */
        public AgentBuilder browserProfile(BrowserProfile profile) {
            this.browserProfile = profile;
            return this;
        }

        /**
         * Set the agent configuration.
         */
        public AgentBuilder config(AgentConfig config) {
            this.agentConfig = config;
            return this;
        }

        /**
         * Set a custom action registry with additional or custom actions.
         */
        public AgentBuilder actionRegistry(ActionRegistry registry) {
            this.actionRegistry = registry;
            return this;
        }

        /**
         * Provide an existing browser session for the agent to use.
         */
        public AgentBuilder browserSession(BrowserSession session) {
            this.browserSession = session;
            return this;
        }

        /**
         * Build and run the agent, returning the result.
         * This is a convenience method that creates the agent, runs it, and closes it.
         *
         * @return the agent execution result
         */
        public AgentResult run() {
            Agent.Builder builder = new Agent.Builder()
                    .task(task)
                    .llmProvider(llmProvider);

            if (browserProfile != null) {
                builder.browserProfile(browserProfile);
            }
            if (agentConfig != null) {
                builder.config(agentConfig);
            }
            if (actionRegistry != null) {
                builder.actionRegistry(actionRegistry);
            }
            if (browserSession != null) {
                builder.browserSession(browserSession);
            }

            try (Agent agent = builder.build()) {
                return agent.run();
            }
        }

        /**
         * Build the agent without running it.
         * Caller is responsible for calling run() and close().
         *
         * @return the configured Agent instance
         */
        public Agent build() {
            Agent.Builder builder = new Agent.Builder()
                    .task(task)
                    .llmProvider(llmProvider);

            if (browserProfile != null) {
                builder.browserProfile(browserProfile);
            }
            if (agentConfig != null) {
                builder.config(agentConfig);
            }
            if (actionRegistry != null) {
                builder.actionRegistry(actionRegistry);
            }
            if (browserSession != null) {
                builder.browserSession(browserSession);
            }

            return builder.build();
        }
    }
}
