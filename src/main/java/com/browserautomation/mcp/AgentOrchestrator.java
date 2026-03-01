package com.browserautomation.mcp;

import com.browserautomation.agent.AgentResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Multi-Agent Control Plane (MCP) for coordinating multiple agents.
 * Equivalent to browser-use's mcp module.
 *
 * Manages agent registration, task routing, inter-agent communication,
 * and workflow coordination across multiple browser automation agents.
 */
public class AgentOrchestrator implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(AgentOrchestrator.class);

    private final Map<String, AgentInfo> agents;
    private final List<WorkflowStep> workflow;
    private final Map<String, Object> sharedContext;
    private final List<AgentMessage> messageQueue;
    private boolean running;

    public AgentOrchestrator() {
        this.agents = new ConcurrentHashMap<>();
        this.workflow = new ArrayList<>();
        this.sharedContext = new ConcurrentHashMap<>();
        this.messageQueue = new ArrayList<>();
        this.running = false;
    }

    /**
     * Register an agent with the orchestrator.
     *
     * @param name         unique name for the agent
     * @param capabilities list of capabilities this agent provides
     * @param description  human-readable description
     */
    public void registerAgent(String name, List<String> capabilities, String description) {
        agents.put(name, new AgentInfo(name, capabilities, description));
        logger.info("Registered agent: {} (capabilities: {})", name, capabilities);
    }

    /**
     * Unregister an agent.
     */
    public boolean unregisterAgent(String name) {
        AgentInfo removed = agents.remove(name);
        if (removed != null) {
            logger.info("Unregistered agent: {}", name);
            return true;
        }
        return false;
    }

    /**
     * Find agents that have a specific capability.
     *
     * @param capability the required capability
     * @return list of agent names with that capability
     */
    public List<String> findAgentsByCapability(String capability) {
        List<String> matching = new ArrayList<>();
        for (AgentInfo info : agents.values()) {
            if (info.capabilities.contains(capability)) {
                matching.add(info.name);
            }
        }
        return matching;
    }

    /**
     * Get information about a specific agent.
     */
    public Optional<AgentInfo> getAgentInfo(String name) {
        return Optional.ofNullable(agents.get(name));
    }

    /**
     * Get all registered agents.
     */
    public Map<String, AgentInfo> getAllAgents() {
        return new LinkedHashMap<>(agents);
    }

    /**
     * Add a step to the workflow.
     *
     * @param agentName the agent to execute the step
     * @param task      the task for the agent
     * @param dependsOn names of steps that must complete first
     */
    public void addWorkflowStep(String agentName, String task, List<String> dependsOn) {
        String stepId = "step-" + (workflow.size() + 1);
        workflow.add(new WorkflowStep(stepId, agentName, task, dependsOn != null ? dependsOn : List.of()));
        logger.info("Added workflow step: {} -> {} (depends on: {})", stepId, agentName, dependsOn);
    }

    /**
     * Get the current workflow steps.
     */
    public List<WorkflowStep> getWorkflow() {
        return new ArrayList<>(workflow);
    }

    /**
     * Clear the workflow.
     */
    public void clearWorkflow() {
        workflow.clear();
    }

    /**
     * Put a value into the shared context accessible by all agents.
     */
    public void setSharedContext(String key, Object value) {
        sharedContext.put(key, value);
    }

    /**
     * Get a value from the shared context.
     */
    public Object getSharedContext(String key) {
        return sharedContext.get(key);
    }

    /**
     * Get all shared context.
     */
    public Map<String, Object> getAllSharedContext() {
        return new LinkedHashMap<>(sharedContext);
    }

    /**
     * Send a message from one agent to another.
     */
    public void sendMessage(String fromAgent, String toAgent, String content) {
        AgentMessage msg = new AgentMessage(fromAgent, toAgent, content, System.currentTimeMillis());
        synchronized (messageQueue) {
            messageQueue.add(msg);
        }
        logger.debug("Message from {} to {}: {}", fromAgent, toAgent, content);
    }

    /**
     * Get pending messages for an agent.
     */
    public List<AgentMessage> getMessages(String agentName) {
        List<AgentMessage> messages = new ArrayList<>();
        synchronized (messageQueue) {
            messageQueue.removeIf(msg -> {
                if (msg.toAgent.equals(agentName)) {
                    messages.add(msg);
                    return true;
                }
                return false;
            });
        }
        return messages;
    }

    /**
     * Get the number of registered agents.
     */
    public int getAgentCount() {
        return agents.size();
    }

    @Override
    public void close() {
        running = false;
        agents.clear();
        workflow.clear();
        sharedContext.clear();
        synchronized (messageQueue) {
            messageQueue.clear();
        }
    }

    /**
     * Information about a registered agent.
     */
    public static class AgentInfo {
        private final String name;
        private final List<String> capabilities;
        private final String description;
        private AgentResult lastResult;

        public AgentInfo(String name, List<String> capabilities, String description) {
            this.name = name;
            this.capabilities = List.copyOf(capabilities);
            this.description = description;
        }

        public String getName() { return name; }
        public List<String> getCapabilities() { return capabilities; }
        public String getDescription() { return description; }
        public AgentResult getLastResult() { return lastResult; }
        public void setLastResult(AgentResult result) { this.lastResult = result; }

        @Override
        public String toString() {
            return String.format("Agent[%s: %s, capabilities=%s]", name, description, capabilities);
        }
    }

    /**
     * A step in a multi-agent workflow.
     */
    public static class WorkflowStep {
        private final String stepId;
        private final String agentName;
        private final String task;
        private final List<String> dependsOn;
        private boolean completed;
        private AgentResult result;

        public WorkflowStep(String stepId, String agentName, String task, List<String> dependsOn) {
            this.stepId = stepId;
            this.agentName = agentName;
            this.task = task;
            this.dependsOn = List.copyOf(dependsOn);
            this.completed = false;
        }

        public String getStepId() { return stepId; }
        public String getAgentName() { return agentName; }
        public String getTask() { return task; }
        public List<String> getDependsOn() { return dependsOn; }
        public boolean isCompleted() { return completed; }
        public void setCompleted(boolean completed) { this.completed = completed; }
        public AgentResult getResult() { return result; }
        public void setResult(AgentResult result) { this.result = result; }
    }

    /**
     * A message between agents.
     */
    public static class AgentMessage {
        private final String fromAgent;
        private final String toAgent;
        private final String content;
        private final long timestamp;

        public AgentMessage(String fromAgent, String toAgent, String content, long timestamp) {
            this.fromAgent = fromAgent;
            this.toAgent = toAgent;
            this.content = content;
            this.timestamp = timestamp;
        }

        public String getFromAgent() { return fromAgent; }
        public String getToAgent() { return toAgent; }
        public String getContent() { return content; }
        public long getTimestamp() { return timestamp; }
    }
}
