package com.browserautomation.agent;

import com.browserautomation.action.ActionRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SystemPromptTest {

    @Test
    void testBuildDefaultPrompt() {
        AgentConfig config = new AgentConfig();
        ActionRegistry registry = new ActionRegistry();
        SystemPrompt prompt = new SystemPrompt(config, registry);

        String result = prompt.build();
        assertNotNull(result);
        assertTrue(result.contains("navigate"));
        assertTrue(result.contains("click"));
        assertTrue(result.contains("done"));
        assertTrue(result.contains("5")); // max actions per step
    }

    @Test
    void testBuildWithOverride() {
        AgentConfig config = new AgentConfig().overrideSystemMessage("Custom prompt");
        ActionRegistry registry = new ActionRegistry();
        SystemPrompt prompt = new SystemPrompt(config, registry);

        String result = prompt.build();
        assertEquals("Custom prompt", result);
    }

    @Test
    void testBuildWithExtension() {
        AgentConfig config = new AgentConfig().extendSystemMessage("Extra rules here");
        ActionRegistry registry = new ActionRegistry();
        SystemPrompt prompt = new SystemPrompt(config, registry);

        String result = prompt.build();
        assertTrue(result.contains("Extra rules here"));
    }

    @Test
    void testBuildWithOverrideAndExtension() {
        AgentConfig config = new AgentConfig()
                .overrideSystemMessage("Base prompt")
                .extendSystemMessage("Extra");
        ActionRegistry registry = new ActionRegistry();
        SystemPrompt prompt = new SystemPrompt(config, registry);

        String result = prompt.build();
        assertTrue(result.startsWith("Base prompt"));
        assertTrue(result.contains("Extra"));
    }
}
