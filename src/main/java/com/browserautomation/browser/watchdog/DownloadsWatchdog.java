package com.browserautomation.browser.watchdog;

import com.browserautomation.browser.BrowserSession;
import com.browserautomation.event.BrowserEvents;
import com.browserautomation.event.EventBus;
import com.microsoft.playwright.Download;
import com.microsoft.playwright.Page;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Watchdog that manages the full download lifecycle via Playwright download events.
 * Tracks downloads, auto-saves to configured directory, and dispatches events.
 */
public class DownloadsWatchdog extends BaseWatchdog {

    private final BrowserSession session;
    private final Path downloadDirectory;
    private final List<DownloadInfo> downloads = new CopyOnWriteArrayList<>();

    public DownloadsWatchdog(EventBus eventBus, BrowserSession session, Path downloadDirectory) {
        super(eventBus);
        this.session = session;
        this.downloadDirectory = downloadDirectory;
    }

    @Override
    public String getWatchdogName() { return "downloads"; }

    @Override
    protected void subscribeToEvents() {
        eventBus.subscribe(BrowserEvents.NavigateToUrlEvent.class, event -> setupDownloadHandler());
        setupDownloadHandler();
    }

    private void setupDownloadHandler() {
        if (!session.isStarted()) return;
        try {
            Page page = session.getCurrentPage();
            if (page == null) return;

            page.onDownload(download -> handleDownload(download));
        } catch (Exception e) {
            logger.debug("Download handler setup error: {}", e.getMessage());
        }
    }

    private void handleDownload(Download download) {
        String url = download.url();
        String suggestedFilename = download.suggestedFilename();
        logger.info("Download started: {} ({})", suggestedFilename, url);

        dispatchEvent(new BrowserEvents.DownloadStartedEvent(url, suggestedFilename));

        DownloadInfo info = new DownloadInfo(url, suggestedFilename);
        downloads.add(info);

        try {
            Path savePath = downloadDirectory.resolve(suggestedFilename);
            download.saveAs(savePath);
            info.completedPath = savePath.toString();
            info.completed = true;
            logger.info("Download completed: {}", savePath);
            dispatchEvent(new BrowserEvents.DownloadCompletedEvent(savePath.toString()));
        } catch (Exception e) {
            info.error = e.getMessage();
            logger.warn("Download failed: {}", e.getMessage());
        }
    }

    public List<DownloadInfo> getDownloads() { return List.copyOf(downloads); }
    public int getDownloadCount() { return downloads.size(); }

    public static class DownloadInfo {
        private final String url;
        private final String suggestedFilename;
        private String completedPath;
        private boolean completed;
        private String error;

        public DownloadInfo(String url, String suggestedFilename) {
            this.url = url;
            this.suggestedFilename = suggestedFilename;
        }

        public String getUrl() { return url; }
        public String getSuggestedFilename() { return suggestedFilename; }
        public String getCompletedPath() { return completedPath; }
        public boolean isCompleted() { return completed; }
        public String getError() { return error; }
    }
}
