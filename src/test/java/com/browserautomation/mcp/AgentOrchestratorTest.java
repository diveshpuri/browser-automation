package com.browserautomation.mcp;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for AgentOrchestrator.
 */
class AgentOrchestratorTest {

    private AgentOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        orchestrator = new AgentOrchestrator();
    }

    @AfterEach
    void tearDown() {
        orchestrator.close();
    }

    @Test
    void testRegisterAgent() {
        orchestrator.registerAgent("agent1", List.of("search", "click"), "Search agent");
        assertEquals(1, orchestrator.getAgentCount());
    }

    @Test
    void testUnregisterAgent() {
        orchestrator.registerAgent("agent1", List.of("search"), "Agent 1");
        assertTrue(orchestrator.unregisterAgent("agent1"));
        assertEquals(0, orchestrator.getAgentCount());
    }

    @Test
    void testUnregisterNonexistent() {
        assertFalse(orchestrator.unregisterAgent("none"));
    }

    @Test
    void testGetAgentInfo() {
        orchestrator.registerAgent("agent1", List.of("search", "navigate"), "Agent 1");
        Optional<AgentOrchestrator.AgentInfo> info = orchestrator.getAgentInfo("agent1");
        assertTrue(info.isPresent());
        assertEquals("agent1", info.get().getName());
        assertEquals(List.of("search", "navigate"), info.get().getCapabilities());
        assertEquals("Agent 1", info.get().getDescription());
    }

    @Test
    void testGetAgentInfoNotFound() {
        assertFalse(orchestrator.getAgentInfo("none").isPresent());
    }

    @Test
    void testFindAgentsByCapability() {
        orchestrator.registerAgent("agent1", List.of("search", "click"), "A1");
        orchestrator.registerAgent("agent2", List.of("navigate", "click"), "A2");
        orchestrator.registerAgent("agent3", List.of("search"), "A3");

        List<String> clickAgents = orchestrator.findAgentsByCapability("click");
        assertEquals(2, clickAgents.size());
        assertTrue(clickAgents.contains("agent1"));
        assertTrue(clickAgents.contains("agent2"));

        List<String> navAgents = orchestrator.findAgentsByCapability("navigate");
        assertEquals(1, navAgents.size());
    }

    @Test
    void testGetAllAgents() {
        orchestrator.registerAgent("a1", List.of("cap1"), "Agent 1");
        orchestrator.registerAgent("a2", List.of("cap2"), "Agent 2");
        assertEquals(2, orchestrator.getAllAgents().size());
    }

    @Test
    void testWorkflow() {
        orchestrator.addWorkflowStep("agent1", "navigate to page", null);
        orchestrator.addWorkflowStep("agent2", "extract data", List.of("step-1"));

        List<AgentOrchestrator.WorkflowStep> workflow = orchestrator.getWorkflow();
        assertEquals(2, workflow.size());
        assertEquals("step-1", workflow.get(0).getStepId());
        assertEquals("agent1", workflow.get(0).getAgentName());
        assertEquals("navigate to page", workflow.get(0).getTask());
        assertTrue(workflow.get(0).getDependsOn().isEmpty());
        assertEquals("step-2", workflow.get(1).getStepId());
        assertEquals(List.of("step-1"), workflow.get(1).getDependsOn());
    }

    @Test
    void testClearWorkflow() {
        orchestrator.addWorkflowStep("agent1", "task1", null);
        orchestrator.clearWorkflow();
        assertTrue(orchestrator.getWorkflow().isEmpty());
    }

    @Test
    void testWorkflowStepCompletion() {
        orchestrator.addWorkflowStep("agent1", "task1", null);
        AgentOrchestrator.WorkflowStep step = orchestrator.getWorkflow().get(0);
        assertFalse(step.isCompleted());
        step.setCompleted(true);
        assertTrue(step.isCompleted());
    }

    @Test
    void testSharedContext() {
        orchestrator.setSharedContext("key1", "value1");
        orchestrator.setSharedContext("key2", 42);
        assertEquals("value1", orchestrator.getSharedContext("key1"));
        assertEquals(42, orchestrator.getSharedContext("key2"));
        assertNull(orchestrator.getSharedContext("missing"));
    }

    @Test
    void testGetAllSharedContext() {
        orchestrator.setSharedContext("a", 1);
        orchestrator.setSharedContext("b", 2);
        assertEquals(2, orchestrator.getAllSharedContext().size());
    }

    @Test
    void testMessaging() {
        orchestrator.sendMessage("agent1", "agent2", "hello");
        orchestrator.sendMessage("agent1", "agent2", "world");
        orchestrator.sendMessage("agent3", "agent1", "hi");

        List<AgentOrchestrator.AgentMessage> agent2Messages = orchestrator.getMessages("agent2");
        assertEquals(2, agent2Messages.size());
        assertEquals("hello", agent2Messages.get(0).getContent());
        assertEquals("agent1", agent2Messages.get(0).getFromAgent());
        assertEquals("agent2", agent2Messages.get(0).getToAgent());
        assertTrue(agent2Messages.get(0).getTimestamp() > 0);

        // Messages are consumed once retrieved
        assertTrue(orchestrator.getMessages("agent2").isEmpty());

        // Agent1 should still have its message
        assertEquals(1, orchestrator.getMessages("agent1").size());
    }

    @Test
    void testClose() {
        orchestrator.registerAgent("a1", List.of("cap"), "Agent");
        orchestrator.addWorkflowStep("a1", "task", null);
        orchestrator.setSharedContext("key", "val");
        orchestrator.sendMessage("a1", "a2", "msg");

        orchestrator.close();

        assertEquals(0, orchestrator.getAgentCount());
        assertTrue(orchestrator.getWorkflow().isEmpty());
        assertTrue(orchestrator.getAllSharedContext().isEmpty());
    }

    @Test
    void testAgentInfoToString() {
        orchestrator.registerAgent("agent1", List.of("search"), "Search Agent");
        AgentOrchestrator.AgentInfo info = orchestrator.getAgentInfo("agent1").get();
        String str = info.toString();
        assertTrue(str.contains("agent1"));
        assertTrue(str.contains("Search Agent"));
        assertTrue(str.contains("search"));
    }
}
