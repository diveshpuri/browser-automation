package com.browserautomation.browser.watchdog;

import com.browserautomation.browser.BrowserSession;
import com.browserautomation.event.BrowserEvents;
import com.browserautomation.event.EventBus;
import com.microsoft.playwright.Page;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Watchdog that handles JavaScript dialogs (alert, confirm, prompt, beforeunload).
 */
public class DialogWatchdog extends BaseWatchdog {

    public enum DialogAction { ACCEPT, DISMISS, ACCEPT_WITH_TEXT }

    private final BrowserSession session;
    private DialogAction defaultAction = DialogAction.DISMISS;
    private String defaultResponseText = "";
    private final List<DialogInfo> dialogHistory = new CopyOnWriteArrayList<>();

    public DialogWatchdog(EventBus eventBus, BrowserSession session) {
        super(eventBus);
        this.session = session;
    }

    @Override
    public String getWatchdogName() { return "dialog"; }

    @Override
    protected void subscribeToEvents() {
        setupDialogHandler();
    }

    private void setupDialogHandler() {
        if (!session.isStarted()) return;
        try {
            Page page = session.getCurrentPage();
            if (page == null) return;

            page.onDialog(dialog -> {
                String type = dialog.type();
                String message = dialog.message();
                logger.info("Dialog detected: type={}, message='{}'", type, message);

                DialogInfo info = new DialogInfo(type, message);
                dialogHistory.add(info);

                dispatchEvent(new BrowserEvents.DialogDetectedEvent(type, message));

                switch (defaultAction) {
                    case ACCEPT -> dialog.accept();
                    case DISMISS -> dialog.dismiss();
                    case ACCEPT_WITH_TEXT -> dialog.accept(defaultResponseText);
                }
            });
        } catch (Exception e) {
            logger.warn("Failed to set up dialog handler: {}", e.getMessage());
        }
    }

    public void setDefaultAction(DialogAction action) { this.defaultAction = action; }
    public void setDefaultResponseText(String text) { this.defaultResponseText = text; }
    public List<DialogInfo> getDialogHistory() { return List.copyOf(dialogHistory); }

    public static class DialogInfo {
        private final String type;
        private final String message;
        private final long timestamp;

        public DialogInfo(String type, String message) {
            this.type = type;
            this.message = message;
            this.timestamp = System.currentTimeMillis();
        }

        public String getType() { return type; }
        public String getMessage() { return message; }
        public long getTimestamp() { return timestamp; }
    }
}
