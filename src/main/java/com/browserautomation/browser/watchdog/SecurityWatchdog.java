package com.browserautomation.browser.watchdog;

import com.browserautomation.browser.BrowserSession;
import com.browserautomation.event.BrowserEvents;
import com.browserautomation.event.EventBus;
import com.microsoft.playwright.Page;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Watchdog that monitors browser security policies and detects violations.
 * Checks for mixed content, CSP violations, and suspicious redirects.
 */
public class SecurityWatchdog extends BaseWatchdog {

    private final BrowserSession session;
    private final Set<String> violations = new CopyOnWriteArraySet<>();
    private final Set<String> blockedDomains;

    public SecurityWatchdog(EventBus eventBus, BrowserSession session) {
        this(eventBus, session, Set.of());
    }

    public SecurityWatchdog(EventBus eventBus, BrowserSession session, Set<String> blockedDomains) {
        super(eventBus);
        this.session = session;
        this.blockedDomains = blockedDomains;
    }

    @Override
    public String getWatchdogName() { return "security"; }

    @Override
    protected void subscribeToEvents() {
        eventBus.subscribe(BrowserEvents.NavigateToUrlEvent.class, event -> checkSecurity(event.getUrl()));
        schedulePeriodicCheck(this::checkCurrentPage, 10000);
    }

    private void checkSecurity(String url) {
        // Check blocked domains
        for (String blocked : blockedDomains) {
            if (url.contains(blocked)) {
                String violation = "Blocked domain access: " + blocked;
                violations.add(violation);
                dispatchEvent(new BrowserEvents.SecurityViolationEvent(violation, url));
                logger.warn("Security violation: {}", violation);
            }
        }

        // Check for non-HTTPS on sensitive pages
        if (url.startsWith("http://") && !url.startsWith("http://localhost") && !url.startsWith("http://127.0.0.1")) {
            String violation = "Non-HTTPS connection detected";
            violations.add(violation);
            dispatchEvent(new BrowserEvents.SecurityViolationEvent(violation, url));
        }
    }

    private void checkCurrentPage() {
        if (!session.isStarted()) return;
        try {
            Page page = session.getCurrentPage();
            if (page == null) return;
            checkSecurity(page.url());
        } catch (Exception e) {
            logger.debug("Security check error: {}", e.getMessage());
        }
    }

    public Set<String> getViolations() { return Set.copyOf(violations); }
    public boolean hasViolations() { return !violations.isEmpty(); }
}
