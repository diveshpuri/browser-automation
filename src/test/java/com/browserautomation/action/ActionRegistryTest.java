package com.browserautomation.action;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ActionRegistryTest {

    @Test
    void testDefaultActionsRegistered() {
        ActionRegistry registry = new ActionRegistry();

        assertNotNull(registry.getAction("navigate"));
        assertNotNull(registry.getAction("click"));
        assertNotNull(registry.getAction("input_text"));
        assertNotNull(registry.getAction("scroll"));
        assertNotNull(registry.getAction("go_back"));
        assertNotNull(registry.getAction("switch_tab"));
        assertNotNull(registry.getAction("close_tab"));
        assertNotNull(registry.getAction("open_tab"));
        assertNotNull(registry.getAction("send_keys"));
        assertNotNull(registry.getAction("extract_content"));
        assertNotNull(registry.getAction("screenshot"));
        assertNotNull(registry.getAction("select_dropdown_option"));
        assertNotNull(registry.getAction("get_dropdown_options"));
        assertNotNull(registry.getAction("wait"));
        assertNotNull(registry.getAction("done"));
    }

    @Test
    void testUnknownActionReturnsNull() {
        ActionRegistry registry = new ActionRegistry();
        assertNull(registry.getAction("nonexistent_action"));
    }

    @Test
    void testGetAllActions() {
        ActionRegistry registry = new ActionRegistry();
        assertTrue(registry.getAllActions().size() >= 15);
    }

    @Test
    void testGetActionsDescription() {
        ActionRegistry registry = new ActionRegistry();
        String desc = registry.getActionsDescription();
        assertNotNull(desc);
        assertTrue(desc.contains("navigate"));
        assertTrue(desc.contains("click"));
        assertTrue(desc.contains("done"));
    }

    @Test
    void testGetToolDefinitions() {
        ActionRegistry registry = new ActionRegistry();
        List<Map<String, Object>> tools = registry.getToolDefinitions();
        assertNotNull(tools);
        assertFalse(tools.isEmpty());

        // Verify structure
        Map<String, Object> firstTool = tools.get(0);
        assertEquals("function", firstTool.get("type"));
        assertNotNull(firstTool.get("function"));
    }

    @Test
    void testCustomActionRegistration() {
        ActionRegistry registry = new ActionRegistry();

        BrowserAction customAction = new BrowserAction() {
            @Override
            public String getName() { return "custom_action"; }
            @Override
            public String getDescription() { return "A custom action"; }
            @Override
            public String getParameterSchema() { return "{}"; }
            @Override
            public ActionResult execute(com.browserautomation.browser.BrowserSession session, ActionParameters params) {
                return ActionResult.success("custom result");
            }
        };

        registry.register(customAction);
        assertNotNull(registry.getAction("custom_action"));
    }
}
