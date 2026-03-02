package com.browserautomation.action;

import com.browserautomation.action.actions.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Registry of available browser actions.
 * Manages action registration and lookup.
 */
public class ActionRegistry {

    private static final Logger logger = LoggerFactory.getLogger(ActionRegistry.class);

    private final Map<String, BrowserAction> actions = new LinkedHashMap<>();

    public ActionRegistry() {
        registerDefaultActions();
    }

    /**
     * Register all built-in browser actions.
     */
    private void registerDefaultActions() {
        logger.info("[REGISTRY] Registering default browser actions...");
        register(new NavigateAction());
        register(new ClickElementAction());
        register(new InputTextAction());
        register(new ScrollAction());
        register(new GoBackAction());
        register(new SwitchTabAction());
        register(new CloseTabAction());
        register(new OpenTabAction());
        register(new SendKeysAction());
        register(new ExtractContentAction());
        register(new ScreenshotAction());
        register(new SelectDropdownAction());
        register(new GetDropdownOptionsAction());
        register(new WaitAction());
        register(new DoneAction());
        register(new HoverAction());
        register(new DragAndDropAction());
        register(new MouseMoveAction());
        logger.info("[REGISTRY] {} default actions registered: {}", actions.size(), actions.keySet());
    }

    /**
     * Register a custom action.
     */
    public void register(BrowserAction action) {
        actions.put(action.getName(), action);
        logger.debug("Registered action: {}", action.getName());
    }

    /**
     * Get an action by name.
     */
    public BrowserAction getAction(String name) {
        BrowserAction action = actions.get(name);
        if (action == null) {
            logger.warn("[REGISTRY] Action '{}' not found. Available actions: {}", name, actions.keySet());
        }
        return action;
    }

    /**
     * Get all registered actions as a collection.
     */
    public Collection<BrowserAction> getAllActions() {
        return Collections.unmodifiableCollection(actions.values());
    }

    /**
     * Get all registered actions as a name-to-action map.
     */
    public Map<String, BrowserAction> getAllActionsMap() {
        return Collections.unmodifiableMap(actions);
    }

    /**
     * Get the action names and descriptions for the LLM system prompt.
     */
    public String getActionsDescription() {
        StringBuilder sb = new StringBuilder();
        for (BrowserAction action : actions.values()) {
            sb.append("- **").append(action.getName()).append("**: ").append(action.getDescription()).append("\n");
            sb.append("  Parameters: ").append(action.getParameterSchema()).append("\n\n");
        }
        return sb.toString();
    }

    /**
     * Get the tool definitions in a format suitable for LLM function/tool calling.
     */
    public List<Map<String, Object>> getToolDefinitions() {
        List<Map<String, Object>> tools = new ArrayList<>();
        for (BrowserAction action : actions.values()) {
            Map<String, Object> tool = new LinkedHashMap<>();
            tool.put("type", "function");

            Map<String, Object> function = new LinkedHashMap<>();
            function.put("name", action.getName());
            function.put("description", action.getDescription());
            function.put("parameters", action.getParameterSchema());

            tool.put("function", function);
            tools.add(tool);
        }
        return tools;
    }
}
