package com.browserautomation.scriptgen;

/**
 * Configuration for the Playwright TypeScript script generator.
 *
 * Controls timeouts, viewport settings, wait strategies, and
 * other parameters that affect the generated script behavior.
 */
public class ScriptGeneratorConfig {

    private int viewportWidth = 1280;
    private int viewportHeight = 720;
    private long defaultTimeoutMs = 30000;
    private long navigationTimeoutMs = 60000;
    private long networkIdleTimeoutMs = 10000;
    private long domStableTimeoutMs = 2000;
    private String userAgent;
    private boolean includeComments = true;
    private boolean includeRetryLogic = true;
    private boolean includeNetworkWaits = true;
    private boolean includeDomStabilityWaits = true;

    public ScriptGeneratorConfig() {
    }

    // Builder-style methods

    public ScriptGeneratorConfig viewportWidth(int width) {
        this.viewportWidth = width;
        return this;
    }

    public ScriptGeneratorConfig viewportHeight(int height) {
        this.viewportHeight = height;
        return this;
    }

    public ScriptGeneratorConfig defaultTimeout(long ms) {
        this.defaultTimeoutMs = ms;
        return this;
    }

    public ScriptGeneratorConfig navigationTimeout(long ms) {
        this.navigationTimeoutMs = ms;
        return this;
    }

    public ScriptGeneratorConfig networkIdleTimeout(long ms) {
        this.networkIdleTimeoutMs = ms;
        return this;
    }

    public ScriptGeneratorConfig domStableTimeout(long ms) {
        this.domStableTimeoutMs = ms;
        return this;
    }

    public ScriptGeneratorConfig userAgent(String ua) {
        this.userAgent = ua;
        return this;
    }

    public ScriptGeneratorConfig includeComments(boolean include) {
        this.includeComments = include;
        return this;
    }

    public ScriptGeneratorConfig includeRetryLogic(boolean include) {
        this.includeRetryLogic = include;
        return this;
    }

    public ScriptGeneratorConfig includeNetworkWaits(boolean include) {
        this.includeNetworkWaits = include;
        return this;
    }

    public ScriptGeneratorConfig includeDomStabilityWaits(boolean include) {
        this.includeDomStabilityWaits = include;
        return this;
    }

    // Getters

    public int getViewportWidth() { return viewportWidth; }
    public int getViewportHeight() { return viewportHeight; }
    public long getDefaultTimeoutMs() { return defaultTimeoutMs; }
    public long getNavigationTimeoutMs() { return navigationTimeoutMs; }
    public long getNetworkIdleTimeoutMs() { return networkIdleTimeoutMs; }
    public long getDomStableTimeoutMs() { return domStableTimeoutMs; }
    public String getUserAgent() { return userAgent; }
    public boolean isIncludeComments() { return includeComments; }
    public boolean isIncludeRetryLogic() { return includeRetryLogic; }
    public boolean isIncludeNetworkWaits() { return includeNetworkWaits; }
    public boolean isIncludeDomStabilityWaits() { return includeDomStabilityWaits; }
}
