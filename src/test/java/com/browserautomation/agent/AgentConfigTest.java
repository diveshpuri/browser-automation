package com.browserautomation.agent;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AgentConfigTest {

    @Test
    void testDefaultValues() {
        AgentConfig config = new AgentConfig();
        assertEquals(50, config.getMaxSteps());
        assertEquals(5, config.getMaxFailures());
        assertEquals(5, config.getMaxActionsPerStep());
        assertTrue(config.isUseVision());
        assertTrue(config.isUseThinking());
        assertFalse(config.isGenerateGif());
        assertNull(config.getOverrideSystemMessage());
        assertNull(config.getExtendSystemMessage());
    }

    @Test
    void testFluentApi() {
        AgentConfig config = new AgentConfig()
                .maxSteps(20)
                .maxFailures(3)
                .maxActionsPerStep(10)
                .useVision(false)
                .useThinking(false)
                .extendSystemMessage("Extra instructions")
                .llmTimeoutSeconds(90)
                .stepTimeoutSeconds(300);

        assertEquals(20, config.getMaxSteps());
        assertEquals(3, config.getMaxFailures());
        assertEquals(10, config.getMaxActionsPerStep());
        assertFalse(config.isUseVision());
        assertFalse(config.isUseThinking());
        assertEquals("Extra instructions", config.getExtendSystemMessage());
        assertEquals(90, config.getLlmTimeoutSeconds());
        assertEquals(300, config.getStepTimeoutSeconds());
    }
}
