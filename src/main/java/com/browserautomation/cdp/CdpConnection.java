package com.browserautomation.cdp;

import com.microsoft.playwright.CDPSession;
import com.microsoft.playwright.Page;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.function.Consumer;

/**
 * Manages Chrome DevTools Protocol (CDP) direct connections.
 * Provides low-level access to CDP commands for enhanced element interaction,
 * DOM snapshots, network monitoring, and more.
 *
 */
public class CdpConnection implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(CdpConnection.class);

    private CDPSession cdpSession;
    private final Page page;

    public CdpConnection(Page page) {
        this.page = page;
    }

    /**
     * Initialize the CDP session from a Playwright page.
     */
    public void connect() {
        try {
            this.cdpSession = page.context().newCDPSession(page);
            logger.info("CDP session established");
        } catch (Exception e) {
            logger.warn("Failed to create CDP session: {}", e.getMessage());
        }
    }

    /**
     * Send a CDP command and return the result.
     */
    @SuppressWarnings("unchecked")
    public <T> T sendCommand(String method, Map<String, Object> params) {
        ensureConnected();
        try {
            com.google.gson.JsonObject jsonParams = new com.google.gson.JsonObject();
            if (params != null) {
                for (Map.Entry<String, Object> entry : params.entrySet()) {
                    Object value = entry.getValue();
                    if (value instanceof String) {
                        jsonParams.addProperty(entry.getKey(), (String) value);
                    } else if (value instanceof Number) {
                        jsonParams.addProperty(entry.getKey(), (Number) value);
                    } else if (value instanceof Boolean) {
                        jsonParams.addProperty(entry.getKey(), (Boolean) value);
                    }
                }
            }
            com.google.gson.JsonObject result = cdpSession.send(method, jsonParams);
            return (T) result;
        } catch (Exception e) {
            logger.warn("CDP command {} failed: {}", method, e.getMessage());
            return null;
        }
    }

    /**
     * Send a CDP command with no params.
     */
    public <T> T sendCommand(String method) {
        return sendCommand(method, null);
    }

    /**
     * Subscribe to a CDP event.
     */
    public void on(String eventName, Consumer<com.google.gson.JsonObject> handler) {
        ensureConnected();
        cdpSession.on(eventName, handler);
    }

    /**
     * Detach the CDP session.
     */
    public void detach() {
        if (cdpSession != null) {
            try {
                cdpSession.detach();
                logger.debug("CDP session detached");
            } catch (Exception e) {
                logger.debug("CDP detach error: {}", e.getMessage());
            }
        }
    }

    public boolean isConnected() {
        return cdpSession != null;
    }

    public CDPSession getCdpSession() { return cdpSession; }

    private void ensureConnected() {
        if (cdpSession == null) {
            throw new IllegalStateException("CDP session not connected. Call connect() first.");
        }
    }

    @Override
    public void close() {
        detach();
    }
}
