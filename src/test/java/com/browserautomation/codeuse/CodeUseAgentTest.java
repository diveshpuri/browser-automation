package com.browserautomation.codeuse;

import com.browserautomation.llm.LlmProvider;
import com.browserautomation.llm.LlmResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

class CodeUseAgentTest {

    private LlmProvider mockProvider;
    private CodeUseAgent agent;

    @BeforeEach
    void setUp() {
        mockProvider = mock(LlmProvider.class);
        agent = new CodeUseAgent(mockProvider, CodeUseAgent.Language.PYTHON, 10000);
    }

    @Test
    void testExecuteCellSuccess() {
        CodeUseAgent.CellResult result = agent.executeCell("print('Hello, World!')");

        assertTrue(result.isSuccess());
        assertEquals("Hello, World!", result.getOutput());
        assertNull(result.getError());
        assertEquals(1, result.cellNumber());
    }

    @Test
    void testExecuteCellFailure() {
        CodeUseAgent.CellResult result = agent.executeCell("raise ValueError('test error')");

        assertFalse(result.isSuccess());
        assertNotNull(result.getError());
    }

    @Test
    void testExecuteCellSyntaxError() {
        CodeUseAgent.CellResult result = agent.executeCell("def incomplete(");

        assertFalse(result.isSuccess());
    }

    @Test
    void testCellHistory() {
        agent.executeCell("print('cell 1')");
        agent.executeCell("print('cell 2')");

        assertEquals(2, agent.getCells().size());
        assertEquals(1, agent.getCells().get(0).number());
        assertEquals(2, agent.getCells().get(1).number());
    }

    @Test
    void testGetLastResult() {
        assertNull(agent.getLastResult());

        agent.executeCell("print('test')");
        assertNotNull(agent.getLastResult());
        assertTrue(agent.getLastResult().isSuccess());
    }

    @Test
    void testSetAndGetVariable() {
        agent.setVariable("x", "42");
        assertEquals("42", agent.getVariable("x"));
    }

    @Test
    void testGetNamespace() {
        agent.setVariable("a", "1");
        agent.setVariable("b", "2");

        Map<String, Object> ns = agent.getNamespace();
        assertEquals(2, ns.size());
        assertTrue(ns.containsKey("a"));
        assertTrue(ns.containsKey("b"));
    }

    @Test
    void testVariableTracking() {
        agent.executeCell("x = 42\ny = 'hello'");

        // Variables should be tracked from assignments
        assertNotNull(agent.getVariable("x"));
        assertNotNull(agent.getVariable("y"));
    }

    @Test
    void testReset() {
        agent.executeCell("print('test')");
        agent.setVariable("x", "42");

        agent.reset();

        assertTrue(agent.getCells().isEmpty());
        assertTrue(agent.getNamespace().isEmpty());
        assertNull(agent.getLastResult());
    }

    @Test
    void testSetEnvironmentVariable() {
        agent.setEnvironmentVariable("MY_VAR", "test_value");
        CodeUseAgent.CellResult result = agent.executeCell("import os; print(os.environ.get('MY_VAR', 'not_set'))");

        assertTrue(result.isSuccess());
        assertEquals("test_value", result.getOutput());
    }

    @Test
    void testGetLanguage() {
        assertEquals(CodeUseAgent.Language.PYTHON, agent.getLanguage());
    }

    @Test
    void testExecuteTask() {
        when(mockProvider.chatCompletion(anyList()))
                .thenReturn(new LlmResponse(
                        "```python\nprint(2 + 2)\n```", null, 50, 30));

        CodeUseAgent.CellResult result = agent.executeTask("Calculate 2 + 2");

        assertTrue(result.isSuccess());
        assertEquals("4", result.getOutput());
    }

    @Test
    void testExecuteTaskLlmFailure() {
        when(mockProvider.chatCompletion(anyList()))
                .thenThrow(new RuntimeException("LLM error"));

        CodeUseAgent.CellResult result = agent.executeTask("Do something");

        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("Failed to generate code"));
    }

    @Test
    void testExecuteTaskNoCode() {
        when(mockProvider.chatCompletion(anyList()))
                .thenReturn(new LlmResponse("I cannot generate code for this.", null, 50, 30));

        CodeUseAgent.CellResult result = agent.executeTask("Something impossible");

        // The LLM response might still be treated as code or fail gracefully
        assertNotNull(result);
    }

    @Test
    void testMultipleCells() {
        CodeUseAgent.CellResult r1 = agent.executeCell("print('first')");
        CodeUseAgent.CellResult r2 = agent.executeCell("print('second')");
        CodeUseAgent.CellResult r3 = agent.executeCell("print('third')");

        assertTrue(r1.isSuccess());
        assertTrue(r2.isSuccess());
        assertTrue(r3.isSuccess());
        assertEquals(3, agent.getCells().size());
    }

    @Test
    void testCellResultToString() {
        CodeUseAgent.CellResult success = new CodeUseAgent.CellResult(1, "print('hi')", true, "hi", null, 100);
        assertTrue(success.toString().contains("Out [1]"));

        CodeUseAgent.CellResult failure = new CodeUseAgent.CellResult(2, "bad code", false, "", "SyntaxError", 50);
        assertTrue(failure.toString().contains("Error [2]"));
    }

    @Test
    void testLanguageEnum() {
        assertEquals("python3", CodeUseAgent.Language.PYTHON.getCommand());
        assertEquals(".py", CodeUseAgent.Language.PYTHON.getExtension());
        assertEquals("node", CodeUseAgent.Language.JAVASCRIPT.getCommand());
        assertEquals(".js", CodeUseAgent.Language.JAVASCRIPT.getExtension());
        assertEquals("bash", CodeUseAgent.Language.BASH.getCommand());
        assertEquals(".sh", CodeUseAgent.Language.BASH.getExtension());
    }

    @Test
    void testClose() {
        // Should not throw
        agent.close();
    }
}
