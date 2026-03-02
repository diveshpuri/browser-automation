package com.browserautomation.agent;

import com.browserautomation.browser.BrowserSession;
import com.browserautomation.event.BrowserEvent;
import com.browserautomation.event.BrowserEvents;
import com.browserautomation.event.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Executes multiple actions per agent step (up to max_actions_per_step).
 * Allows the LLM to specify multiple actions in a single response for efficiency.
 *
 * Equivalent to browser-use's multi-act capability (max_actions_per_step=5).
 */
public class MultiActExecutor {

    private static final Logger logger = LoggerFactory.getLogger(MultiActExecutor.class);

    private final BrowserSession session;
    private final EventBus eventBus;
    private final int maxActionsPerStep;

    public MultiActExecutor(BrowserSession session, EventBus eventBus, int maxActionsPerStep) {
        this.session = session;
        this.eventBus = eventBus;
        this.maxActionsPerStep = maxActionsPerStep;
    }

    /**
     * Execute multiple actions in sequence, stopping on failure.
     * Returns the results of all executed actions.
     */
    public MultiActResult execute(List<ActionRequest> actions,
                                   Function<ActionRequest, ActionResult> actionExecutor) {
        List<ActionResult> results = new ArrayList<>();
        int actionsToExecute = Math.min(actions.size(), maxActionsPerStep);

        logger.info("Executing {} actions (max={} per step)", actionsToExecute, maxActionsPerStep);

        for (int i = 0; i < actionsToExecute; i++) {
            ActionRequest action = actions.get(i);
            logger.debug("Executing action {}/{}: {}", i + 1, actionsToExecute, action.getActionType());

            try {
                ActionResult result = actionExecutor.apply(action);
                results.add(result);

                if (!result.isSuccess()) {
                    logger.warn("Action {}/{} failed: {}", i + 1, actionsToExecute, result.getError());
                    break; // Stop on first failure
                }

                // Check if this is a terminal action (done, extract_content)
                if (action.isTerminal()) {
                    logger.debug("Terminal action reached, stopping multi-act");
                    break;
                }

                // Brief pause between actions
                if (i < actionsToExecute - 1) {
                    Thread.sleep(100);
                }
            } catch (Exception e) {
                results.add(new ActionResult(false, null, e.getMessage()));
                logger.warn("Action {}/{} threw exception: {}", i + 1, actionsToExecute, e.getMessage());
                break;
            }
        }

        boolean allSuccess = results.stream().allMatch(ActionResult::isSuccess);
        return new MultiActResult(results, allSuccess);
    }

    public int getMaxActionsPerStep() { return maxActionsPerStep; }

    /**
     * A request to execute a specific action.
     */
    public static class ActionRequest {
        private final String actionType;
        private final Map<String, Object> parameters;
        private final boolean terminal;

        public ActionRequest(String actionType, Map<String, Object> parameters) {
            this(actionType, parameters, false);
        }

        public ActionRequest(String actionType, Map<String, Object> parameters, boolean terminal) {
            this.actionType = actionType;
            this.parameters = parameters;
            this.terminal = terminal;
        }

        public String getActionType() { return actionType; }
        public Map<String, Object> getParameters() { return parameters; }
        public boolean isTerminal() { return terminal; }

        @SuppressWarnings("unchecked")
        public <T> T getParam(String key) { return (T) parameters.get(key); }

        public <T> T getParam(String key, T defaultValue) {
            Object val = parameters.get(key);
            return val != null ? (T) val : defaultValue;
        }
    }

    /**
     * Result of a single action execution.
     */
    public static class ActionResult {
        private final boolean success;
        private final String output;
        private final String error;

        public ActionResult(boolean success, String output, String error) {
            this.success = success;
            this.output = output;
            this.error = error;
        }

        public static ActionResult success() { return new ActionResult(true, null, null); }
        public static ActionResult success(String output) { return new ActionResult(true, output, null); }
        public static ActionResult failure(String error) { return new ActionResult(false, null, error); }

        public boolean isSuccess() { return success; }
        public String getOutput() { return output; }
        public String getError() { return error; }
    }

    /**
     * Result of executing multiple actions.
     */
    public record MultiActResult(List<ActionResult> results, boolean allSuccess) {
        public int getExecutedCount() { return results.size(); }
        public int getSuccessCount() {
            return (int) results.stream().filter(ActionResult::isSuccess).count();
        }
    }
}
