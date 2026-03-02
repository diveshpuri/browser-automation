package com.browserautomation.agent;

/**
 * Configuration for the browser automation agent.
 */
public class AgentConfig {

    private int maxSteps = 50;
    private int maxFailures = 5;
    private int maxActionsPerStep = 5;
    private boolean useVision = true;
    private boolean useThinking = true;
    private String overrideSystemMessage;
    private String extendSystemMessage;
    private boolean generateGif = false;
    private int llmTimeoutSeconds = 60;
    private int stepTimeoutSeconds = 180;

    public AgentConfig() {
    }

    // Builder-style methods

    public AgentConfig maxSteps(int maxSteps) {
        this.maxSteps = maxSteps;
        return this;
    }

    public AgentConfig maxFailures(int maxFailures) {
        this.maxFailures = maxFailures;
        return this;
    }

    public AgentConfig maxActionsPerStep(int maxActionsPerStep) {
        this.maxActionsPerStep = maxActionsPerStep;
        return this;
    }

    public AgentConfig useVision(boolean useVision) {
        this.useVision = useVision;
        return this;
    }

    public AgentConfig useThinking(boolean useThinking) {
        this.useThinking = useThinking;
        return this;
    }

    public AgentConfig overrideSystemMessage(String message) {
        this.overrideSystemMessage = message;
        return this;
    }

    public AgentConfig extendSystemMessage(String message) {
        this.extendSystemMessage = message;
        return this;
    }

    public AgentConfig generateGif(boolean generateGif) {
        this.generateGif = generateGif;
        return this;
    }

    public AgentConfig llmTimeoutSeconds(int timeout) {
        this.llmTimeoutSeconds = timeout;
        return this;
    }

    public AgentConfig stepTimeoutSeconds(int timeout) {
        this.stepTimeoutSeconds = timeout;
        return this;
    }

    // Getters

    public int getMaxSteps() {
        return maxSteps;
    }

    public int getMaxFailures() {
        return maxFailures;
    }

    public int getMaxActionsPerStep() {
        return maxActionsPerStep;
    }

    public boolean isUseVision() {
        return useVision;
    }

    public boolean isUseThinking() {
        return useThinking;
    }

    public String getOverrideSystemMessage() {
        return overrideSystemMessage;
    }

    public String getExtendSystemMessage() {
        return extendSystemMessage;
    }

    public boolean isGenerateGif() {
        return generateGif;
    }

    public int getLlmTimeoutSeconds() {
        return llmTimeoutSeconds;
    }

    public int getStepTimeoutSeconds() {
        return stepTimeoutSeconds;
    }
}
