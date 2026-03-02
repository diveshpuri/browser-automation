package com.browserautomation.event;

import java.util.Map;

/**
 * Typed browser event classes for the event-driven architecture.
 * Each event type corresponds to a specific browser action or lifecycle event.
 *
 */
public final class BrowserEvents {

    private BrowserEvents() {}

    // ========== Navigation Events ==========

    public static class NavigateToUrlEvent extends BrowserEvent {
        public NavigateToUrlEvent(String url) {
            super("NavigateToUrlEvent", Map.of("url", url));
        }
        public String getUrl() { return get("url"); }
    }

    public static class GoBackEvent extends BrowserEvent {
        public GoBackEvent() {
            super("GoBackEvent");
        }
    }

    public static class RefreshPageEvent extends BrowserEvent {
        public RefreshPageEvent() {
            super("RefreshPageEvent");
        }
    }

    // ========== Element Interaction Events ==========

    public static class ClickElementEvent extends BrowserEvent {
        public ClickElementEvent(int elementIndex) {
            super("ClickElementEvent", Map.of("elementIndex", elementIndex));
        }
        public int getElementIndex() { return get("elementIndex"); }
    }

    public static class TypeTextEvent extends BrowserEvent {
        public TypeTextEvent(int elementIndex, String text) {
            super("TypeTextEvent", Map.of("elementIndex", elementIndex, "text", text));
        }
        public int getElementIndex() { return get("elementIndex"); }
        public String getText() { return get("text"); }
    }

    public static class HoverElementEvent extends BrowserEvent {
        public HoverElementEvent(int elementIndex) {
            super("HoverElementEvent", Map.of("elementIndex", elementIndex));
        }
        public int getElementIndex() { return get("elementIndex"); }
    }

    public static class ScrollEvent extends BrowserEvent {
        public ScrollEvent(boolean down, int pixels) {
            super("ScrollEvent", Map.of("down", down, "pixels", pixels));
        }
        public boolean isDown() { return get("down"); }
        public int getPixels() { return get("pixels"); }
    }

    public static class ScrollToTextEvent extends BrowserEvent {
        public ScrollToTextEvent(String text) {
            super("ScrollToTextEvent", Map.of("text", text));
        }
        public String getText() { return get("text"); }
    }

    public static class ClickDropdownEvent extends BrowserEvent {
        public ClickDropdownEvent(int elementIndex, String value) {
            super("ClickDropdownEvent", Map.of("elementIndex", elementIndex, "value", value));
        }
        public int getElementIndex() { return get("elementIndex"); }
        public String getValue() { return get("value"); }
    }

    public static class UploadFileEvent extends BrowserEvent {
        public UploadFileEvent(int elementIndex, String filePath) {
            super("UploadFileEvent", Map.of("elementIndex", elementIndex, "filePath", filePath));
        }
        public int getElementIndex() { return get("elementIndex"); }
        public String getFilePath() { return get("filePath"); }
    }

    public static class DragAndDropEvent extends BrowserEvent {
        public DragAndDropEvent(int sourceIndex, int targetIndex) {
            super("DragAndDropEvent", Map.of("sourceIndex", sourceIndex, "targetIndex", targetIndex));
        }
        public int getSourceIndex() { return get("sourceIndex"); }
        public int getTargetIndex() { return get("targetIndex"); }
    }

    public static class SendKeysEvent extends BrowserEvent {
        public SendKeysEvent(String keys) {
            super("SendKeysEvent", Map.of("keys", keys));
        }
        public String getKeys() { return get("keys"); }
    }

    // ========== Tab Events ==========

    public static class SwitchTabEvent extends BrowserEvent {
        public SwitchTabEvent(int tabIndex) {
            super("SwitchTabEvent", Map.of("tabIndex", tabIndex));
        }
        public int getTabIndex() { return get("tabIndex"); }
    }

    public static class OpenNewTabEvent extends BrowserEvent {
        public OpenNewTabEvent(String url) {
            super("OpenNewTabEvent", Map.of("url", url));
        }
        public String getUrl() { return get("url"); }
    }

    public static class CloseTabEvent extends BrowserEvent {
        public CloseTabEvent(int tabIndex) {
            super("CloseTabEvent", Map.of("tabIndex", tabIndex));
        }
        public int getTabIndex() { return get("tabIndex"); }
    }

    // ========== Agent Lifecycle Events ==========

    public static class AgentStartedEvent extends BrowserEvent {
        public AgentStartedEvent(String task, int maxSteps) {
            super("AgentStartedEvent", Map.of("task", task, "maxSteps", maxSteps));
        }
        public String getTask() { return get("task"); }
        public int getMaxSteps() { return get("maxSteps"); }
    }

    public static class AgentStepStartEvent extends BrowserEvent {
        public AgentStepStartEvent(int stepNumber) {
            super("AgentStepStartEvent", Map.of("stepNumber", stepNumber));
        }
        public int getStepNumber() { return get("stepNumber"); }
    }

    public static class AgentStepCompletedEvent extends BrowserEvent {
        public AgentStepCompletedEvent(int stepNumber, boolean success) {
            super("AgentStepCompletedEvent", Map.of("stepNumber", stepNumber, "success", success));
        }
        public int getStepNumber() { return get("stepNumber"); }
        public boolean isSuccess() { return get("success"); }
    }

    public static class AgentCompletedEvent extends BrowserEvent {
        public AgentCompletedEvent(String result, int totalSteps) {
            super("AgentCompletedEvent", Map.of("result", result, "totalSteps", totalSteps));
        }
        public String getResult() { return get("result"); }
        public int getTotalSteps() { return get("totalSteps"); }
    }

    public static class AgentErrorEvent extends BrowserEvent {
        public AgentErrorEvent(String error, int stepNumber) {
            super("AgentErrorEvent", Map.of("error", error, "stepNumber", stepNumber));
        }
        public String getError() { return get("error"); }
        public int getStepNumber() { return get("stepNumber"); }
    }

    // ========== DOM Events ==========

    public static class DomStateExtractedEvent extends BrowserEvent {
        public DomStateExtractedEvent(int elementCount, String url) {
            super("DomStateExtractedEvent", Map.of("elementCount", elementCount, "url", url));
        }
        public int getElementCount() { return get("elementCount"); }
        public String getUrl() { return get("url"); }
    }

    public static class ScreenshotTakenEvent extends BrowserEvent {
        public ScreenshotTakenEvent(String url) {
            super("ScreenshotTakenEvent", Map.of("url", url));
        }
        public String getUrl() { return get("url"); }
    }

    // ========== Watchdog Events ==========

    public static class CaptchaDetectedEvent extends BrowserEvent {
        public CaptchaDetectedEvent(String url, String captchaType) {
            super("CaptchaDetectedEvent", Map.of("url", url, "captchaType", captchaType));
        }
        public String getUrl() { return get("url"); }
        public String getCaptchaType() { return get("captchaType"); }
    }

    public static class DownloadStartedEvent extends BrowserEvent {
        public DownloadStartedEvent(String url, String suggestedFilename) {
            super("DownloadStartedEvent", Map.of("url", url, "suggestedFilename", suggestedFilename));
        }
        public String getUrl() { return get("url"); }
        public String getSuggestedFilename() { return get("suggestedFilename"); }
    }

    public static class DownloadCompletedEvent extends BrowserEvent {
        public DownloadCompletedEvent(String path) {
            super("DownloadCompletedEvent", Map.of("path", path));
        }
        public String getPath() { return get("path"); }
    }

    public static class BrowserCrashEvent extends BrowserEvent {
        public BrowserCrashEvent(String reason) {
            super("BrowserCrashEvent", Map.of("reason", reason));
        }
        public String getReason() { return get("reason"); }
    }

    public static class DialogDetectedEvent extends BrowserEvent {
        public DialogDetectedEvent(String dialogType, String message) {
            super("DialogDetectedEvent", Map.of("dialogType", dialogType, "message", message));
        }
        public String getDialogType() { return get("dialogType"); }
        public String getMessage() { return get("message"); }
    }

    public static class PermissionRequestedEvent extends BrowserEvent {
        public PermissionRequestedEvent(String permission) {
            super("PermissionRequestedEvent", Map.of("permission", permission));
        }
        public String getPermission() { return get("permission"); }
    }

    public static class StorageStateSavedEvent extends BrowserEvent {
        public StorageStateSavedEvent(String path) {
            super("StorageStateSavedEvent", Map.of("path", path));
        }
        public String getPath() { return get("path"); }
    }

    public static class SecurityViolationEvent extends BrowserEvent {
        public SecurityViolationEvent(String violation, String url) {
            super("SecurityViolationEvent", Map.of("violation", violation, "url", url));
        }
        public String getViolation() { return get("violation"); }
        public String getUrl() { return get("url"); }
    }

    public static class HarRecordingEvent extends BrowserEvent {
        public HarRecordingEvent(String action, String path) {
            super("HarRecordingEvent", Map.of("action", action, "path", path));
        }
        public String getAction() { return get("action"); }
        public String getPath() { return get("path"); }
    }

    // ========== LLM Events ==========

    public static class LlmRequestEvent extends BrowserEvent {
        public LlmRequestEvent(String provider, String model, int inputTokens) {
            super("LlmRequestEvent", Map.of("provider", provider, "model", model, "inputTokens", inputTokens));
        }
        public String getProvider() { return get("provider"); }
        public String getModel() { return get("model"); }
        public int getInputTokens() { return get("inputTokens"); }
    }

    public static class LlmResponseEvent extends BrowserEvent {
        public LlmResponseEvent(String provider, int outputTokens, long latencyMs) {
            super("LlmResponseEvent", Map.of("provider", provider, "outputTokens", outputTokens, "latencyMs", latencyMs));
        }
        public String getProvider() { return get("provider"); }
        public int getOutputTokens() { return get("outputTokens"); }
        public long getLatencyMs() { return get("latencyMs"); }
    }

    public static class LlmErrorEvent extends BrowserEvent {
        public LlmErrorEvent(String provider, String error, boolean willFallback) {
            super("LlmErrorEvent", Map.of("provider", provider, "error", error, "willFallback", willFallback));
        }
        public String getProvider() { return get("provider"); }
        public String getError() { return get("error"); }
        public boolean isWillFallback() { return get("willFallback"); }
    }

    public static class MessageCompactedEvent extends BrowserEvent {
        public MessageCompactedEvent(int originalCount, int compactedCount, int tokensSaved) {
            super("MessageCompactedEvent", Map.of("originalCount", originalCount, "compactedCount", compactedCount, "tokensSaved", tokensSaved));
        }
        public int getOriginalCount() { return get("originalCount"); }
        public int getCompactedCount() { return get("compactedCount"); }
        public int getTokensSaved() { return get("tokensSaved"); }
    }

    public static class LoopDetectedEvent extends BrowserEvent {
        public LoopDetectedEvent(int loopCount, String lastAction) {
            super("LoopDetectedEvent", Map.of("loopCount", loopCount, "lastAction", lastAction));
        }
        public int getLoopCount() { return get("loopCount"); }
        public String getLastAction() { return get("lastAction"); }
    }
}
