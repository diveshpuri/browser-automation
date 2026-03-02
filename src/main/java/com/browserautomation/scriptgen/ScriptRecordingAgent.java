package com.browserautomation.scriptgen;

import com.browserautomation.action.ActionParameters;
import com.browserautomation.action.ActionRegistry;
import com.browserautomation.action.ActionResult;
import com.browserautomation.action.BrowserAction;
import com.browserautomation.agent.Agent;
import com.browserautomation.agent.AgentResult;
import com.browserautomation.browser.BrowserSession;
import com.browserautomation.dom.DomElement;
import com.browserautomation.dom.DomService;
import com.browserautomation.dom.DomState;
import com.browserautomation.dom.SelectorScorer;
import com.browserautomation.dom.SelectorStrategy;
import com.browserautomation.dom.ShadowDomService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

/**
 * A wrapper that records browser actions during agent execution
 * and generates a Playwright TypeScript script from them.
 *
 * Usage:
 * <pre>
 * ScriptRecordingAgent recorder = new ScriptRecordingAgent(agent);
 * AgentResult result = recorder.runAndRecord();
 * String script = recorder.generateScript("Find flights from NYC to London");
 * recorder.saveScript(Path.of("output.spec.ts"));
 * </pre>
 */
public class ScriptRecordingAgent {

    private static final Logger logger = LoggerFactory.getLogger(ScriptRecordingAgent.class);

    private final Agent agent;
    private final PlaywrightScriptGenerator generator;
    private final DomService domService;
    private AgentResult lastResult;

    public ScriptRecordingAgent(Agent agent) {
        this(agent, new PlaywrightScriptGenerator());
    }

    public ScriptRecordingAgent(Agent agent, PlaywrightScriptGenerator generator) {
        this.agent = agent;
        this.generator = generator;
        this.domService = new ShadowDomService();
    }

    /**
     * Install recording wrappers on the action registry.
     * This intercepts action executions to record them for script generation.
     *
     * @param registry the action registry to wrap
     * @return a new registry with recording wrappers
     */
    public ActionRegistry wrapRegistry(ActionRegistry registry) {
        ActionRegistry wrapped = new ActionRegistry();
        for (Map.Entry<String, BrowserAction> entry : registry.getAllActionsMap().entrySet()) {
            String name = entry.getKey();
            BrowserAction original = entry.getValue();
            wrapped.register(new RecordingAction(name, original, generator));
        }
        return wrapped;
    }

    /**
     * Generate a Playwright script from the agent result.
     *
     * @param task the task description
     * @return the TypeScript script
     */
    public String generateScript(String task) {
        if (lastResult != null) {
            return generator.generateFromAgentResult(lastResult, task);
        }
        return generator.generateScript();
    }

    /**
     * Save the generated script to a file.
     */
    public Path saveScript(String task, Path outputPath) throws IOException {
        String script = generateScript(task);
        return generator.generateAndSave(lastResult, task, outputPath);
    }

    /**
     * Get the underlying script generator.
     */
    public PlaywrightScriptGenerator getGenerator() {
        return generator;
    }

    /**
     * Set the last agent result (for generating scripts from results).
     */
    public void setLastResult(AgentResult result) {
        this.lastResult = result;
    }

    /**
     * A wrapper action that records the execution and delegates to the original.
     */
    private static class RecordingAction implements BrowserAction {
        private final String name;
        private final BrowserAction delegate;
        private final PlaywrightScriptGenerator generator;

        RecordingAction(String name, BrowserAction delegate, PlaywrightScriptGenerator generator) {
            this.name = name;
            this.delegate = delegate;
            this.generator = generator;
        }

        @Override
        public String getName() {
            return delegate.getName();
        }

        @Override
        public String getDescription() {
            return delegate.getDescription();
        }

        @Override
        public String getParameterSchema() {
            return delegate.getParameterSchema();
        }

        @Override
        public ActionResult execute(BrowserSession session, ActionParameters params) {
            // Record the action before executing
            String url = null;
            String selector = null;
            try {
                if (session.isStarted() && session.getCurrentPage() != null) {
                    url = session.getCurrentPage().url();
                    // Try to get the best scored selector for element-based actions
                    int index = params.getInt("index", -1);
                    if (index >= 0) {
                        ShadowDomService ds = new ShadowDomService();
                        DomState state = ds.extractState(session.getCurrentPage());
                        DomElement element = state.getElementByIndex(index);
                        if (element != null) {
                            // Use the scored selector for the best result
                            selector = element.buildScoredSelector(element.isInShadowDom());
                            // If in shadow DOM, prepend the shadow host selector
                            if (element.isInShadowDom() && element.getShadowHostSelector() != null) {
                                selector = element.getShadowHostSelector() + " >> " + selector;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // Don't let recording errors affect the actual execution
            }

            generator.recordAction(delegate.getName(), params.toMap(), selector, url);

            // Execute the original action
            return delegate.execute(session, params);
        }
    }
}
