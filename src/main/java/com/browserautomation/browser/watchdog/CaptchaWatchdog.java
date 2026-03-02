package com.browserautomation.browser.watchdog;

import com.browserautomation.browser.BrowserSession;
import com.browserautomation.event.BrowserEvents;
import com.browserautomation.event.EventBus;
import com.microsoft.playwright.Page;

import java.util.List;

/**
 * Watchdog that detects captchas (reCAPTCHA, hCaptcha, Cloudflare) on pages
 * and waits for the user to solve them before continuing.
 */
public class CaptchaWatchdog extends BaseWatchdog {

    private final BrowserSession session;
    private volatile boolean captchaDetected;
    private volatile boolean waitingForSolution;

    private static final List<String> CAPTCHA_INDICATORS = List.of(
            "iframe[src*='recaptcha']",
            "iframe[src*='hcaptcha']",
            ".g-recaptcha",
            ".h-captcha",
            "#cf-turnstile",
            "iframe[src*='challenges.cloudflare.com']",
            "[data-sitekey]",
            "#captcha",
            ".captcha"
    );

    public CaptchaWatchdog(EventBus eventBus, BrowserSession session) {
        super(eventBus);
        this.session = session;
    }

    @Override
    public String getWatchdogName() { return "captcha"; }

    @Override
    protected void subscribeToEvents() {
        eventBus.subscribe(BrowserEvents.NavigateToUrlEvent.class, event -> checkForCaptcha());
        eventBus.subscribe(BrowserEvents.AgentStepCompletedEvent.class, event -> checkForCaptcha());
        schedulePeriodicCheck(this::checkForCaptcha, 3000);
    }

    private void checkForCaptcha() {
        if (!session.isStarted()) return;
        try {
            Page page = session.getCurrentPage();
            if (page == null) return;

            for (String selector : CAPTCHA_INDICATORS) {
                Object count = page.evaluate(
                        "(sel) => document.querySelectorAll(sel).length", selector);
                if (count instanceof Number && ((Number) count).intValue() > 0) {
                    if (!captchaDetected) {
                        captchaDetected = true;
                        String captchaType = detectCaptchaType(selector);
                        logger.info("Captcha detected: {} on {}", captchaType, page.url());
                        dispatchEvent(new BrowserEvents.CaptchaDetectedEvent(page.url(), captchaType));
                    }
                    return;
                }
            }
            captchaDetected = false;
        } catch (Exception e) {
            logger.debug("Captcha check error: {}", e.getMessage());
        }
    }

    private String detectCaptchaType(String selector) {
        if (selector.contains("recaptcha")) return "reCAPTCHA";
        if (selector.contains("hcaptcha")) return "hCaptcha";
        if (selector.contains("cloudflare") || selector.contains("turnstile")) return "Cloudflare Turnstile";
        return "unknown";
    }

    public boolean isCaptchaDetected() { return captchaDetected; }
    public boolean isWaitingForSolution() { return waitingForSolution; }

    /**
     * Wait for captcha to be solved (blocks until solved or timeout).
     */
    public boolean waitForSolution(long timeoutMs) {
        waitingForSolution = true;
        long startTime = System.currentTimeMillis();
        while (captchaDetected && System.currentTimeMillis() - startTime < timeoutMs) {
            try {
                Thread.sleep(1000);
                checkForCaptcha();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        waitingForSolution = false;
        return !captchaDetected;
    }
}
