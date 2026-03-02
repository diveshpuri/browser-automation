package com.browserautomation.browser;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration profile for the browser instance.
 */
public class BrowserProfile {

    private boolean headless = true;
    private int viewportWidth = 1280;
    private int viewportHeight = 720;
    private String userAgent;
    private Path userDataDir;
    private Path downloadsPath;
    private boolean disableSecurity = false;
    private boolean acceptDownloads = true;
    private List<String> args = new ArrayList<>();
    private Map<String, String> extraHeaders = new HashMap<>();
    private String channel;
    private ProxySettings proxy;
    private boolean demoMode = false;
    private int waitBetweenActionsMs = 500;
    private int minimumPageLoadWaitMs = 500;
    private int networkIdleWaitMs = 1000;
    private List<String> allowedDomains = new ArrayList<>();
    private List<String> prohibitedDomains = new ArrayList<>();
    private BrowserEngineType engineType = BrowserEngineType.PLAYWRIGHT;

    public BrowserProfile() {
    }

    // Builder-style methods

    public BrowserProfile headless(boolean headless) {
        this.headless = headless;
        return this;
    }

    public BrowserProfile viewportSize(int width, int height) {
        this.viewportWidth = width;
        this.viewportHeight = height;
        return this;
    }

    public BrowserProfile userAgent(String userAgent) {
        this.userAgent = userAgent;
        return this;
    }

    public BrowserProfile userDataDir(Path userDataDir) {
        this.userDataDir = userDataDir;
        return this;
    }

    public BrowserProfile downloadsPath(Path downloadsPath) {
        this.downloadsPath = downloadsPath;
        return this;
    }

    public BrowserProfile disableSecurity(boolean disableSecurity) {
        this.disableSecurity = disableSecurity;
        return this;
    }

    public BrowserProfile acceptDownloads(boolean acceptDownloads) {
        this.acceptDownloads = acceptDownloads;
        return this;
    }

    public BrowserProfile addArg(String arg) {
        this.args.add(arg);
        return this;
    }

    public BrowserProfile addHeader(String name, String value) {
        this.extraHeaders.put(name, value);
        return this;
    }

    public BrowserProfile channel(String channel) {
        this.channel = channel;
        return this;
    }

    public BrowserProfile proxy(ProxySettings proxy) {
        this.proxy = proxy;
        return this;
    }

    public BrowserProfile demoMode(boolean demoMode) {
        this.demoMode = demoMode;
        return this;
    }

    public BrowserProfile waitBetweenActionsMs(int ms) {
        this.waitBetweenActionsMs = ms;
        return this;
    }

    public BrowserProfile minimumPageLoadWaitMs(int ms) {
        this.minimumPageLoadWaitMs = ms;
        return this;
    }

    public BrowserProfile networkIdleWaitMs(int ms) {
        this.networkIdleWaitMs = ms;
        return this;
    }

    public BrowserProfile allowedDomains(List<String> domains) {
        this.allowedDomains = new ArrayList<>(domains);
        return this;
    }

    public BrowserProfile prohibitedDomains(List<String> domains) {
        this.prohibitedDomains = new ArrayList<>(domains);
        return this;
    }

    /**
     * Set the browser engine type (PLAYWRIGHT or SELENIUM).
     * Default is PLAYWRIGHT.
     *
     * @param engineType the engine type to use
     */
    public BrowserProfile engineType(BrowserEngineType engineType) {
        this.engineType = engineType;
        return this;
    }

    /**
     * Shorthand to use Selenium as the browser engine.
     */
    public BrowserProfile useSelenium() {
        this.engineType = BrowserEngineType.SELENIUM;
        return this;
    }

    /**
     * Shorthand to use Playwright as the browser engine.
     */
    public BrowserProfile usePlaywright() {
        this.engineType = BrowserEngineType.PLAYWRIGHT;
        return this;
    }

    // Getters

    public boolean isHeadless() {
        return headless;
    }

    public int getViewportWidth() {
        return viewportWidth;
    }

    public int getViewportHeight() {
        return viewportHeight;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public Path getUserDataDir() {
        return userDataDir;
    }

    public Path getDownloadsPath() {
        return downloadsPath;
    }

    public boolean isDisableSecurity() {
        return disableSecurity;
    }

    public boolean isAcceptDownloads() {
        return acceptDownloads;
    }

    public List<String> getArgs() {
        return args;
    }

    public Map<String, String> getExtraHeaders() {
        return extraHeaders;
    }

    public String getChannel() {
        return channel;
    }

    public ProxySettings getProxy() {
        return proxy;
    }

    public boolean isDemoMode() {
        return demoMode;
    }

    public int getWaitBetweenActionsMs() {
        return waitBetweenActionsMs;
    }

    public int getMinimumPageLoadWaitMs() {
        return minimumPageLoadWaitMs;
    }

    public int getNetworkIdleWaitMs() {
        return networkIdleWaitMs;
    }

    public List<String> getAllowedDomains() {
        return allowedDomains;
    }

    public List<String> getProhibitedDomains() {
        return prohibitedDomains;
    }

    public BrowserEngineType getEngineType() {
        return engineType;
    }

    /**
     * Proxy configuration settings.
     */
    public static class ProxySettings {
        private final String server;
        private final String username;
        private final String password;

        public ProxySettings(String server) {
            this(server, null, null);
        }

        public ProxySettings(String server, String username, String password) {
            this.server = server;
            this.username = username;
            this.password = password;
        }

        public String getServer() {
            return server;
        }

        public String getUsername() {
            return username;
        }

        public String getPassword() {
            return password;
        }
    }
}
