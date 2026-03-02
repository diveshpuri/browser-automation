package com.browserautomation.browser.watchdog;

import com.browserautomation.browser.BrowserSession;
import com.browserautomation.event.BrowserEvents;
import com.browserautomation.event.EventBus;
import com.microsoft.playwright.BrowserContext;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Watchdog that auto-grants browser permissions (geolocation, notifications, etc.).
 */
public class PermissionsWatchdog extends BaseWatchdog {

    private final BrowserSession session;
    private final Set<String> grantedPermissions = new CopyOnWriteArraySet<>();
    private final Set<String> autoGrantPermissions;

    private static final Set<String> DEFAULT_AUTO_GRANT = Set.of(
            "geolocation", "notifications", "camera", "microphone",
            "clipboard-read", "clipboard-write"
    );

    public PermissionsWatchdog(EventBus eventBus, BrowserSession session) {
        this(eventBus, session, DEFAULT_AUTO_GRANT);
    }

    public PermissionsWatchdog(EventBus eventBus, BrowserSession session, Set<String> autoGrantPermissions) {
        super(eventBus);
        this.session = session;
        this.autoGrantPermissions = autoGrantPermissions;
    }

    @Override
    public String getWatchdogName() { return "permissions"; }

    @Override
    protected void subscribeToEvents() {
        eventBus.subscribe(BrowserEvents.NavigateToUrlEvent.class, event -> grantPermissions());
    }

    private void grantPermissions() {
        if (!session.isStarted()) return;
        try {
            BrowserContext context = session.getContext();
            if (context == null) return;

            for (String permission : autoGrantPermissions) {
                if (!grantedPermissions.contains(permission)) {
                    try {
                        context.grantPermissions(List.of(permission));
                        grantedPermissions.add(permission);
                        logger.debug("Granted permission: {}", permission);
                        dispatchEvent(new BrowserEvents.PermissionRequestedEvent(permission));
                    } catch (Exception e) {
                        logger.debug("Could not grant permission {}: {}", permission, e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("Permission grant error: {}", e.getMessage());
        }
    }

    public Set<String> getGrantedPermissions() { return Set.copyOf(grantedPermissions); }
}
