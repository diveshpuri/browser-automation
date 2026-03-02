package com.browserautomation.browser.watchdog;

import com.browserautomation.browser.BrowserSession;
import com.browserautomation.event.BrowserEvents;
import com.browserautomation.event.EventBus;
import com.microsoft.playwright.Page;

import java.util.Map;

/**
 * Watchdog that monitors DOM state changes, tracks network requests,
 * and detects pagination patterns. Works alongside DomService for
 * enhanced DOM capture with parallel screenshot + DOM extraction.
 */
public class DomWatchdog extends BaseWatchdog {

    private final BrowserSession session;
    private volatile int lastElementCount;
    private volatile String lastUrl;
    private volatile boolean paginationDetected;
    private volatile int pendingNetworkRequests;

    public DomWatchdog(EventBus eventBus, BrowserSession session) {
        super(eventBus);
        this.session = session;
    }

    @Override
    public String getWatchdogName() { return "dom"; }

    @Override
    protected void subscribeToEvents() {
        eventBus.subscribe(BrowserEvents.DomStateExtractedEvent.class, event -> {
            lastElementCount = event.getElementCount();
            lastUrl = event.getUrl();
        });
        eventBus.subscribe(BrowserEvents.NavigateToUrlEvent.class, event -> {
            lastUrl = event.getUrl();
            detectPagination();
        });
        schedulePeriodicCheck(this::trackNetworkActivity, 2000);
    }

    private void detectPagination() {
        if (!session.isStarted()) return;
        try {
            Page page = session.getCurrentPage();
            if (page == null) return;

            Object result = page.evaluate("""
                () => {
                    const paginationSelectors = [
                        'nav[aria-label*="pagination"]', '.pagination', '[class*="pager"]',
                        'a[rel="next"]', 'button[aria-label*="next"]', '[class*="page-number"]'
                    ];
                    for (const sel of paginationSelectors) {
                        if (document.querySelector(sel)) return true;
                    }
                    return false;
                }
            """);
            paginationDetected = Boolean.TRUE.equals(result);
        } catch (Exception e) {
            logger.debug("Pagination detection error: {}", e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void trackNetworkActivity() {
        if (!session.isStarted()) return;
        try {
            Page page = session.getCurrentPage();
            if (page == null) return;

            Object result = page.evaluate("""
                () => {
                    const entries = performance.getEntriesByType('resource');
                    const pending = entries.filter(e => e.responseEnd === 0).length;
                    return { pending: pending, total: entries.length };
                }
            """);
            if (result instanceof Map) {
                Map<String, Object> map = (Map<String, Object>) result;
                pendingNetworkRequests = ((Number) map.getOrDefault("pending", 0)).intValue();
            }
        } catch (Exception e) {
            logger.debug("Network tracking error: {}", e.getMessage());
        }
    }

    public int getLastElementCount() { return lastElementCount; }
    public String getLastUrl() { return lastUrl; }
    public boolean isPaginationDetected() { return paginationDetected; }
    public int getPendingNetworkRequests() { return pendingNetworkRequests; }
}
