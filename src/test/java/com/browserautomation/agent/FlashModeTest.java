package com.browserautomation.agent;

import com.browserautomation.llm.ChatMessage;
import com.browserautomation.llm.LlmProvider;
import com.browserautomation.llm.LlmResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class FlashModeTest {

    private FlashMode flashMode;

    @BeforeEach
    void setUp() {
        LlmProvider mockProvider = mock(LlmProvider.class);
        flashMode = new FlashMode.Builder()
                .llmProvider(mockProvider)
                .disableVision(true)
                .disableThinking(true)
                .disableEvaluation(true)
                .maxActionsPerStep(10)
                .build();
    }

    @Test
    void testDefaults() {
        assertTrue(flashMode.isVisionDisabled());
        assertTrue(flashMode.isThinkingDisabled());
        assertTrue(flashMode.isEvaluationDisabled());
        assertEquals(10, flashMode.getMaxActionsPerStep());
    }

    @Test
    void testToFlashConfig() {
        AgentConfig base = new AgentConfig()
                .maxSteps(100)
                .maxFailures(10)
                .llmTimeoutSeconds(30)
                .stepTimeoutSeconds(60);

        AgentConfig flashConfig = flashMode.toFlashConfig(base);

        assertEquals(100, flashConfig.getMaxSteps());
        assertEquals(10, flashConfig.getMaxFailures());
        assertFalse(flashConfig.isUseVision());
        assertFalse(flashConfig.isUseThinking());
        assertEquals(10, flashConfig.getMaxActionsPerStep());
        assertNotNull(flashConfig.getOverrideSystemMessage());
    }

    @Test
    void testOptimizeMessagesStripsThinking() {
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(ChatMessage.system("System prompt"));
        messages.add(ChatMessage.user("Do something"));
        messages.add(ChatMessage.assistant("Let me think about this..."));
        messages.add(ChatMessage.user("State update"));

        List<ChatMessage> optimized = flashMode.optimizeMessages(messages);

        // Assistant thinking message should be stripped
        assertEquals(3, optimized.size());
        assertEquals(ChatMessage.Role.SYSTEM, optimized.get(0).getRole());
        assertEquals(ChatMessage.Role.USER, optimized.get(1).getRole());
        assertEquals(ChatMessage.Role.USER, optimized.get(2).getRole());
    }

    @Test
    void testOptimizeMessagesStripsEvaluation() {
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(ChatMessage.system("System prompt"));
        messages.add(ChatMessage.user("Evaluate the previous action"));
        messages.add(ChatMessage.user("Do something else"));

        List<ChatMessage> optimized = flashMode.optimizeMessages(messages);

        // Evaluation message should be stripped
        assertEquals(2, optimized.size());
    }

    @Test
    void testStripThinkingFromResponse() {
        List<LlmResponse.ToolCall> toolCalls = List.of(
                new LlmResponse.ToolCall("1", "click", Map.of("index", 5)));
        LlmResponse response = new LlmResponse("I should click the button", toolCalls, 100, 50);

        LlmResponse stripped = flashMode.stripThinking(response);

        assertNull(stripped.getContent());
        assertTrue(stripped.hasToolCalls());
        assertEquals(1, stripped.getToolCalls().size());
    }

    @Test
    void testStripThinkingNoToolCalls() {
        LlmResponse response = new LlmResponse("Just thinking...", null, 100, 50);
        LlmResponse stripped = flashMode.stripThinking(response);

        // No tool calls, so response is returned as-is
        assertEquals("Just thinking...", stripped.getContent());
    }

    @Test
    void testGetSystemPrompt() {
        String prompt = flashMode.getSystemPrompt();
        assertNotNull(prompt);
        assertTrue(prompt.contains("fast"));
        assertTrue(prompt.contains("tool calls"));
    }

    @Test
    void testFlashModeWithVisionEnabled() {
        FlashMode withVision = new FlashMode.Builder()
                .llmProvider(mock(LlmProvider.class))
                .disableVision(false)
                .disableThinking(true)
                .build();

        assertFalse(withVision.isVisionDisabled());
        assertTrue(withVision.isThinkingDisabled());

        AgentConfig config = withVision.toFlashConfig(new AgentConfig());
        assertTrue(config.isUseVision());
    }
}
