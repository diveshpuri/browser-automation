package com.browserautomation.cdp;

import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * CDP-based network monitoring for tracking pending requests,
 * content types, and download detection.
 */
public class CdpNetworkMonitor {

    private static final Logger logger = LoggerFactory.getLogger(CdpNetworkMonitor.class);

    private final CdpConnection cdp;
    private final List<NetworkRequest> requests = new CopyOnWriteArrayList<>();
    private volatile boolean monitoring;

    public CdpNetworkMonitor(CdpConnection cdp) {
        this.cdp = cdp;
    }

    /**
     * Start monitoring network requests via CDP.
     */
    public void startMonitoring() {
        if (!cdp.isConnected()) return;
        monitoring = true;

        cdp.sendCommand("Network.enable");

        cdp.on("Network.requestWillBeSent", event -> {
            if (!monitoring) return;
            try {
                JsonObject request = event.getAsJsonObject("request");
                String requestId = event.get("requestId").getAsString();
                String url = request.get("url").getAsString();
                String method = request.get("method").getAsString();
                requests.add(new NetworkRequest(requestId, url, method));
            } catch (Exception e) {
                logger.debug("Error handling request event: {}", e.getMessage());
            }
        });

        cdp.on("Network.responseReceived", event -> {
            if (!monitoring) return;
            try {
                String requestId = event.get("requestId").getAsString();
                JsonObject response = event.getAsJsonObject("response");
                int status = response.get("status").getAsInt();
                String mimeType = response.has("mimeType") ? response.get("mimeType").getAsString() : "";

                for (NetworkRequest req : requests) {
                    if (req.getRequestId().equals(requestId)) {
                        req.setStatus(status);
                        req.setMimeType(mimeType);
                        req.setCompleted(true);
                        break;
                    }
                }
            } catch (Exception e) {
                logger.debug("Error handling response event: {}", e.getMessage());
            }
        });

        logger.info("CDP network monitoring started");
    }

    /**
     * Stop monitoring network requests.
     */
    public void stopMonitoring() {
        monitoring = false;
        try {
            cdp.sendCommand("Network.disable");
        } catch (Exception e) {
            logger.debug("Error disabling network: {}", e.getMessage());
        }
        logger.info("CDP network monitoring stopped ({} requests tracked)", requests.size());
    }

    /**
     * Get pending (incomplete) requests.
     */
    public List<NetworkRequest> getPendingRequests() {
        return requests.stream().filter(r -> !r.isCompleted()).toList();
    }

    /**
     * Get all tracked requests.
     */
    public List<NetworkRequest> getAllRequests() { return List.copyOf(requests); }

    /**
     * Get the count of pending requests.
     */
    public int getPendingCount() {
        return (int) requests.stream().filter(r -> !r.isCompleted()).count();
    }

    /**
     * Clear tracked requests.
     */
    public void clearRequests() { requests.clear(); }

    public boolean isMonitoring() { return monitoring; }

    /**
     * Set download behavior via CDP.
     */
    public void setDownloadBehavior(String behavior, String downloadPath) {
        try {
            cdp.sendCommand("Browser.setDownloadBehavior",
                    Map.of("behavior", behavior, "downloadPath", downloadPath));
        } catch (Exception e) {
            logger.debug("Failed to set download behavior: {}", e.getMessage());
        }
    }

    public static class NetworkRequest {
        private final String requestId;
        private final String url;
        private final String method;
        private int status;
        private String mimeType;
        private boolean completed;

        public NetworkRequest(String requestId, String url, String method) {
            this.requestId = requestId;
            this.url = url;
            this.method = method;
        }

        public String getRequestId() { return requestId; }
        public String getUrl() { return url; }
        public String getMethod() { return method; }
        public int getStatus() { return status; }
        public String getMimeType() { return mimeType; }
        public boolean isCompleted() { return completed; }
        public void setStatus(int status) { this.status = status; }
        public void setMimeType(String mimeType) { this.mimeType = mimeType; }
        public void setCompleted(boolean completed) { this.completed = completed; }
    }
}
