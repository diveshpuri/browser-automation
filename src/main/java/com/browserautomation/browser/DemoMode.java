package com.browserautomation.browser;

import com.microsoft.playwright.Page;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Demo mode visualization for browser automation.
 *
 * When enabled, highlights elements before interaction and
 * slows down actions to make the automation visible and understandable.
 */
public class DemoMode {

    private static final Logger logger = LoggerFactory.getLogger(DemoMode.class);

    private boolean enabled;
    private int slowMotionMs;
    private String highlightColor;
    private String highlightBorderWidth;
    private int highlightDurationMs;
    private boolean showActionLabels;
    private boolean showTooltips;

    /**
     * Create demo mode with default settings.
     */
    public DemoMode() {
        this.enabled = false;
        this.slowMotionMs = 1000;
        this.highlightColor = "#FF6B35";
        this.highlightBorderWidth = "3px";
        this.highlightDurationMs = 800;
        this.showActionLabels = true;
        this.showTooltips = true;
    }

    /**
     * Enable demo mode.
     */
    public DemoMode enable() {
        this.enabled = true;
        return this;
    }

    /**
     * Disable demo mode.
     */
    public DemoMode disable() {
        this.enabled = false;
        return this;
    }

    /**
     * Set the slow motion delay between actions.
     */
    public DemoMode slowMotion(int ms) {
        this.slowMotionMs = ms;
        return this;
    }

    /**
     * Set the highlight color (CSS color).
     */
    public DemoMode highlightColor(String color) {
        this.highlightColor = color;
        return this;
    }

    /**
     * Set the highlight border width (CSS value).
     */
    public DemoMode highlightBorderWidth(String width) {
        this.highlightBorderWidth = width;
        return this;
    }

    /**
     * Set how long the highlight stays visible.
     */
    public DemoMode highlightDuration(int ms) {
        this.highlightDurationMs = ms;
        return this;
    }

    /**
     * Toggle action label display.
     */
    public DemoMode showActionLabels(boolean show) {
        this.showActionLabels = show;
        return this;
    }

    /**
     * Toggle tooltip display on hover.
     */
    public DemoMode showTooltips(boolean show) {
        this.showTooltips = show;
        return this;
    }

    /**
     * Highlight a DOM element on the page before performing an action.
     *
     * @param page     the Playwright page
     * @param selector the CSS selector of the element
     * @param action   a description of the action being performed
     */
    public void highlightElement(Page page, String selector, String action) {
        if (!enabled) return;

        try {
            String script = String.format("""
                    (args) => {
                        const el = document.querySelector(args.selector);
                        if (!el) return;
                        
                        // Save original styles
                        const origOutline = el.style.outline;
                        const origOutlineOffset = el.style.outlineOffset;
                        const origTransition = el.style.transition;
                        
                        // Apply highlight
                        el.style.outline = '%s solid %s';
                        el.style.outlineOffset = '2px';
                        el.style.transition = 'outline 0.2s ease-in-out';
                        el.scrollIntoView({behavior: 'smooth', block: 'center'});
                        
                        // Show action label
                        if (args.showLabel && args.action) {
                            const label = document.createElement('div');
                            label.id = 'ba-demo-label';
                            label.textContent = args.action;
                            label.style.cssText = 'position:fixed;top:10px;right:10px;background:%s;color:white;' +
                                'padding:8px 16px;border-radius:6px;font-family:monospace;font-size:14px;z-index:99999;' +
                                'box-shadow:0 2px 8px rgba(0,0,0,0.3);';
                            document.body.appendChild(label);
                            setTimeout(() => label.remove(), %d);
                        }
                        
                        // Show tooltip
                        if (args.showTooltip) {
                            const rect = el.getBoundingClientRect();
                            const tooltip = document.createElement('div');
                            tooltip.id = 'ba-demo-tooltip';
                            tooltip.textContent = el.tagName.toLowerCase() + (el.id ? '#' + el.id : '') + 
                                (el.className ? '.' + el.className.split(' ')[0] : '');
                            tooltip.style.cssText = 'position:fixed;left:' + rect.left + 'px;top:' + (rect.top - 25) + 
                                'px;background:#333;color:white;padding:2px 6px;border-radius:3px;font-size:11px;' +
                                'font-family:monospace;z-index:99999;pointer-events:none;';
                            document.body.appendChild(tooltip);
                            setTimeout(() => tooltip.remove(), %d);
                        }
                        
                        // Remove highlight after delay
                        setTimeout(() => {
                            el.style.outline = origOutline;
                            el.style.outlineOffset = origOutlineOffset;
                            el.style.transition = origTransition;
                        }, %d);
                    }
                    """, highlightBorderWidth, highlightColor, highlightColor,
                    highlightDurationMs, highlightDurationMs, highlightDurationMs);

            page.evaluate(script, java.util.Map.of(
                    "selector", selector,
                    "action", action != null ? action : "",
                    "showLabel", showActionLabels,
                    "showTooltip", showTooltips
            ));

            // Apply slow motion delay
            Thread.sleep(slowMotionMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            logger.debug("Demo mode highlight failed: {}", e.getMessage());
        }
    }

    /**
     * Show a notification banner on the page.
     */
    public void showNotification(Page page, String message) {
        if (!enabled) return;

        try {
            String script = """
                    (msg) => {
                        const existing = document.getElementById('ba-demo-notification');
                        if (existing) existing.remove();
                        const el = document.createElement('div');
                        el.id = 'ba-demo-notification';
                        el.textContent = msg;
                        el.style.cssText = 'position:fixed;bottom:20px;left:50%;transform:translateX(-50%);' +
                            'background:#1a1a2e;color:white;padding:12px 24px;border-radius:8px;font-family:sans-serif;' +
                            'font-size:14px;z-index:99999;box-shadow:0 4px 12px rgba(0,0,0,0.3);';
                        document.body.appendChild(el);
                        setTimeout(() => el.remove(), 3000);
                    }
                    """;
            page.evaluate(script, message);
        } catch (Exception e) {
            logger.debug("Demo notification failed: {}", e.getMessage());
        }
    }

    /**
     * Remove all demo mode overlays from the page.
     */
    public void clearOverlays(Page page) {
        if (!enabled) return;
        try {
            page.evaluate("""
                    () => {
                        ['ba-demo-label', 'ba-demo-tooltip', 'ba-demo-notification'].forEach(id => {
                            const el = document.getElementById(id);
                            if (el) el.remove();
                        });
                    }
                    """);
        } catch (Exception e) {
            logger.debug("Failed to clear overlays: {}", e.getMessage());
        }
    }

    // Getters
    public boolean isEnabled() { return enabled; }
    public int getSlowMotionMs() { return slowMotionMs; }
    public String getHighlightColor() { return highlightColor; }
    public String getHighlightBorderWidth() { return highlightBorderWidth; }
    public int getHighlightDurationMs() { return highlightDurationMs; }
    public boolean isShowActionLabels() { return showActionLabels; }
    public boolean isShowTooltips() { return showTooltips; }
}
