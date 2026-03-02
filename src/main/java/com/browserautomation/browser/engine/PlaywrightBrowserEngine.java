package com.browserautomation.browser.engine;

import com.browserautomation.browser.BrowserProfile;
import com.browserautomation.browser.BrowserState;
import com.browserautomation.dom.DomElement;
import com.browserautomation.dom.DomService;
import com.browserautomation.dom.DomState;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Playwright-based browser engine implementation.
 * This is the default engine, extracted from the original BrowserSession.
 */
public class PlaywrightBrowserEngine implements BrowserEngine {

    private static final Logger logger = LoggerFactory.getLogger(PlaywrightBrowserEngine.class);

    private Playwright playwright;
    private Browser browser;
    private BrowserContext context;
    private Page currentPage;
    private final DomService domService;

    public PlaywrightBrowserEngine() {
        this.domService = new DomService();
    }

    @Override
    public void start(BrowserProfile profile) {
        logger.info("[PLAYWRIGHT] Starting Playwright engine");
        long startTime = System.currentTimeMillis();
        this.playwright = Playwright.create();
        logger.info("[PLAYWRIGHT] Playwright instance created in {}ms", System.currentTimeMillis() - startTime);

        BrowserType.LaunchOptions launchOptions = new BrowserType.LaunchOptions()
                .setHeadless(profile.isHeadless());

        if (!profile.getArgs().isEmpty()) {
            launchOptions.setArgs(profile.getArgs());
        }
        if (profile.getChannel() != null) {
            launchOptions.setChannel(profile.getChannel());
        }

        long browserLaunchStart = System.currentTimeMillis();
        this.browser = playwright.chromium().launch(launchOptions);
        logger.info("[PLAYWRIGHT] Chromium launched in {}ms", System.currentTimeMillis() - browserLaunchStart);

        Browser.NewContextOptions contextOptions = new Browser.NewContextOptions()
                .setViewportSize(profile.getViewportWidth(), profile.getViewportHeight())
                .setAcceptDownloads(profile.isAcceptDownloads());

        if (profile.getUserAgent() != null) {
            contextOptions.setUserAgent(profile.getUserAgent());
        }
        if (profile.getProxy() != null) {
            BrowserProfile.ProxySettings ps = profile.getProxy();
            contextOptions.setProxy(new com.microsoft.playwright.options.Proxy(ps.getServer())
                    .setUsername(ps.getUsername())
                    .setPassword(ps.getPassword()));
        }

        this.context = browser.newContext(contextOptions);

        if (!profile.getExtraHeaders().isEmpty()) {
            context.setExtraHTTPHeaders(profile.getExtraHeaders());
        }

        this.currentPage = context.newPage();
        logger.info("[PLAYWRIGHT] Engine started in {}ms", System.currentTimeMillis() - startTime);
    }

    @Override
    public void startWithVideoRecording(BrowserProfile profile, Path videoDir) {
        logger.info("[PLAYWRIGHT] Starting with video recording to {}", videoDir);
        long startTime = System.currentTimeMillis();
        this.playwright = Playwright.create();

        BrowserType.LaunchOptions launchOptions = new BrowserType.LaunchOptions()
                .setHeadless(profile.isHeadless());
        if (!profile.getArgs().isEmpty()) {
            launchOptions.setArgs(profile.getArgs());
        }
        if (profile.getChannel() != null) {
            launchOptions.setChannel(profile.getChannel());
        }

        this.browser = playwright.chromium().launch(launchOptions);

        Browser.NewContextOptions contextOptions = new Browser.NewContextOptions()
                .setViewportSize(profile.getViewportWidth(), profile.getViewportHeight())
                .setAcceptDownloads(profile.isAcceptDownloads())
                .setRecordVideoDir(videoDir)
                .setRecordVideoSize(profile.getViewportWidth(), profile.getViewportHeight());

        if (profile.getUserAgent() != null) {
            contextOptions.setUserAgent(profile.getUserAgent());
        }
        if (profile.getProxy() != null) {
            BrowserProfile.ProxySettings ps = profile.getProxy();
            contextOptions.setProxy(new com.microsoft.playwright.options.Proxy(ps.getServer())
                    .setUsername(ps.getUsername())
                    .setPassword(ps.getPassword()));
        }

        this.context = browser.newContext(contextOptions);
        if (!profile.getExtraHeaders().isEmpty()) {
            context.setExtraHTTPHeaders(profile.getExtraHeaders());
        }

        this.currentPage = context.newPage();
        logger.info("[PLAYWRIGHT] Engine started with video recording in {}ms", System.currentTimeMillis() - startTime);
    }

    @Override
    public void navigateTo(String url) {
        currentPage.navigate(url);
    }

    @Override
    public void waitForPageLoad(long minimumWaitMs) {
        try {
            currentPage.waitForLoadState(LoadState.DOMCONTENTLOADED);
            Thread.sleep(minimumWaitMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public String getCurrentUrl() {
        return currentPage.url();
    }

    @Override
    public String getPageTitle() {
        return currentPage.title();
    }

    @Override
    public DomState extractDomState() {
        return domService.extractState(currentPage);
    }

    @Override
    public byte[] takeScreenshot() {
        return currentPage.screenshot(new Page.ScreenshotOptions().setFullPage(false));
    }

    @Override
    @SuppressWarnings("unchecked")
    public BrowserState.PageInfo getPageInfo() {
        try {
            Object result = currentPage.evaluate(
                    "() => ({ scrollY: window.scrollY, pageHeight: document.documentElement.scrollHeight, viewportHeight: window.innerHeight })");
            if (result instanceof Map) {
                Map<String, Object> map = (Map<String, Object>) result;
                double scrollY = ((Number) map.get("scrollY")).doubleValue();
                double pageHeight = ((Number) map.get("pageHeight")).doubleValue();
                double viewportHeight = ((Number) map.get("viewportHeight")).doubleValue();
                return new BrowserState.PageInfo(scrollY, pageHeight, viewportHeight);
            }
        } catch (Exception e) {
            logger.warn("[PLAYWRIGHT] Failed to get page info: {}", e.getMessage());
        }
        return new BrowserState.PageInfo(0, 0, 720);
    }

    @Override
    public void clickElement(String selector, DomElement element) {
        try {
            currentPage.locator(selector).first().click(new Locator.ClickOptions().setTimeout(5000));
        } catch (Exception e) {
            logger.warn("[PLAYWRIGHT] Direct click failed on '{}': {} — falling back to JS", selector, e.getMessage());
            currentPage.evaluate("(selector) => { document.querySelector(selector)?.click(); }", selector);
        }
    }

    @Override
    public void typeText(String selector, String text) {
        Locator locator = currentPage.locator(selector).first();
        locator.click();
        locator.fill(text);
    }

    @Override
    public void scroll(boolean down, int pixels) {
        int scrollAmount = down ? pixels : -pixels;
        currentPage.evaluate("(amount) => window.scrollBy(0, amount)", scrollAmount);
    }

    @Override
    public void goBack() {
        currentPage.goBack();
    }

    @Override
    public void switchTab(int tabIndex) {
        List<Page> pages = context.pages();
        if (tabIndex >= 0 && tabIndex < pages.size()) {
            currentPage = pages.get(tabIndex);
            currentPage.bringToFront();
        } else {
            throw new RuntimeException("Invalid tab index: " + tabIndex);
        }
    }

    @Override
    public void closeTab(int tabIndex) {
        List<Page> pages = context.pages();
        if (tabIndex >= 0 && tabIndex < pages.size()) {
            Page pageToClose = pages.get(tabIndex);
            if (pageToClose == currentPage && pages.size() > 1) {
                int newIndex = tabIndex > 0 ? tabIndex - 1 : 1;
                currentPage = pages.get(newIndex);
            }
            pageToClose.close();
        }
    }

    @Override
    public void openNewTab(String url) {
        currentPage = context.newPage();
        if (url != null && !url.isEmpty()) {
            currentPage.navigate(url);
        }
    }

    @Override
    public void sendKeys(String keys) {
        currentPage.keyboard().press(keys);
    }

    @Override
    public String extractContent() {
        return currentPage.innerText("body");
    }

    @Override
    public void uploadFile(String selector, Path filePath) {
        currentPage.locator(selector).first().setInputFiles(filePath);
    }

    @Override
    public void selectDropdownOption(String selector, String value) {
        try {
            currentPage.locator(selector).first().selectOption(value);
        } catch (Exception e) {
            currentPage.locator(selector).first().selectOption(
                    new com.microsoft.playwright.options.SelectOption().setLabel(value));
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<String> getDropdownOptions(String selector) {
        List<String> options = (List<String>) currentPage.evaluate(
                "(sel) => Array.from(document.querySelector(sel)?.options || []).map(o => o.text + ' (' + o.value + ')')",
                selector);
        return options != null ? options : new ArrayList<>();
    }

    @Override
    public void hoverElement(String selector) {
        currentPage.locator(selector).first().hover();
    }

    @Override
    public void dragAndDrop(String sourceSelector, String targetSelector) {
        currentPage.locator(sourceSelector).first().dragTo(currentPage.locator(targetSelector).first());
    }

    @Override
    public void mouseMove(double x, double y) {
        currentPage.mouse().move(x, y);
    }

    @Override
    public Object executeJavaScript(String script) {
        return currentPage.evaluate(script);
    }

    @Override
    public Object executeJavaScript(String script, Object arg) {
        return currentPage.evaluate(script, arg);
    }

    @Override
    public List<TabInfo> getTabsInfo() {
        List<TabInfo> tabs = new ArrayList<>();
        List<Page> pages = context.pages();
        for (int i = 0; i < pages.size(); i++) {
            Page p = pages.get(i);
            tabs.add(new TabInfo(i, p.url(), p.title()));
        }
        return tabs;
    }

    @Override
    public int getActiveTabIndex() {
        List<Page> pages = context.pages();
        for (int i = 0; i < pages.size(); i++) {
            if (pages.get(i) == currentPage) {
                return i;
            }
        }
        return 0;
    }

    @Override
    public boolean isStarted() {
        return browser != null && currentPage != null;
    }

    @Override
    public String getEngineTypeName() {
        return "Playwright";
    }

    /**
     * Get the underlying Playwright Page (for Playwright-specific features).
     */
    public Page getPlaywrightPage() {
        return currentPage;
    }

    /**
     * Get the underlying Playwright BrowserContext (for Playwright-specific features).
     */
    public BrowserContext getPlaywrightContext() {
        return context;
    }

    @Override
    public void close() {
        try {
            if (context != null) {
                context.close();
            }
            if (browser != null) {
                browser.close();
            }
            if (playwright != null) {
                playwright.close();
            }
            logger.info("[PLAYWRIGHT] Engine closed");
        } catch (Exception e) {
            logger.warn("[PLAYWRIGHT] Error closing engine: {}", e.getMessage());
        }
    }
}
