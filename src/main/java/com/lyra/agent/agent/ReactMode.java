package com.lyra.agent.agent;

import com.lyra.agent.event.AgentEvent;
import com.lyra.agent.event.EventBus;
import com.lyra.agent.llm.LLMProvider;
import com.lyra.agent.memory.AgentMemory;
import com.lyra.agent.parser.ReActOutputParser;
import com.lyra.agent.tool.ToolExecutor;
import com.lyra.agent.tool.ToolRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ReAct mode implementation following the Mode interface.
 * Implements the Reasoning + Action pattern with Thought -> Action -> Observation cycles.
 */
public class ReactMode implements Mode {
    private static final Logger logger = LoggerFactory.getLogger(ReactMode.class);
    private final ToolRegistry toolRegistry;
    private final EventBus eventBus;
    private final int maxSteps;
    private final Resource promptTemplate;

    public ReactMode(LLMProvider llmProvider, ToolRegistry toolRegistry, EventBus eventBus, int maxSteps, Resource promptTemplate) {
        this.toolRegistry = toolRegistry;
        this.eventBus = eventBus;
        this.maxSteps = maxSteps;
        this.promptTemplate = promptTemplate;
    }

    @Override
    public String name() {
        return "react";
    }

    @Override
    public ModeResult run(ModeContext context) {
        logger.info("Starting ReAct mode execution with {} initial messages", context.getMessages().size());
        List<Message> messages = new ArrayList<>(context.getMessages());
        AgentMemory memory = context.getMemory();
        List<Trace> trace = new ArrayList<>(context.getTrace());
        int stepCount;

        logger.debug("Publishing agent start event");
        eventBus.publish(new AgentEvent("agent.start", Map.of("mode", name(), "agentId", "default")));

        for (int step = 0; step < maxSteps; step++) {
            stepCount = step + 1;
            logger.info("Starting step {} of {}", stepCount, maxSteps);
            Trace stepTrace = Trace.start("trace-" + step, step, name(), "default", "reasoning", "Starting reasoning step " + step);
            trace.add(stepTrace);

            logger.debug("Publishing agent step event for step {}", stepCount);
            eventBus.publish(new AgentEvent("agent.step", Map.of("step", step + 1, "mode", name())));

            // Build prompt with current state
            logger.debug("Building prompt for step {}", stepCount);
            String prompt = buildPrompt(messages, context.getMemory());
            logger.info("========== ReAct Prompt for Step {} ==========", stepCount);
            logger.info("Prompt length: {} characters", prompt.length());
            logger.info("Full Prompt:\n{}", prompt);
            logger.info("========== End of Prompt ==========");
            List<Message> promptMessages = List.of(Message.user(prompt));

            // Get LLM response
            logger.debug("Calling LLM for step {}", stepCount);
            com.lyra.agent.llm.LlmResponse response;
            String output;
            try {
                response = context.getLlmProvider().chat(promptMessages, context.getOptions());
                output = response.getContent();
                logger.debug("LLM response received for step {}: {}", stepCount, output.substring(0, Math.min(100, output.length())) + (output.length() > 100 ? "..." : ""));
            } catch (Exception e) {
                String errorMsg = "LLM error: " + e.getMessage();
                logger.error(errorMsg, e);
                
                Trace errorTrace = stepTrace.end("error", errorMsg);
                trace.add(errorTrace);
                
                eventBus.publish(new AgentEvent("agent.error", Map.of("message", errorMsg)));
                
                // Return an interrupted result with the error trace
                return ModeResult.interrupted(trace);
            }

            // Check if the output contains an error message before parsing
            if (output.contains("Error calling") || output.contains("Too Many Requests") || output.contains("429")) {
                String errorMsg = "LLM API Error: " + output;
                logger.error(errorMsg);
                
                Trace errorTrace = stepTrace.end("error", errorMsg);
                trace.add(errorTrace);
                
                eventBus.publish(new AgentEvent("agent.error", Map.of("message", errorMsg)));
                
                // Return an interrupted result with the error trace
                return ModeResult.interrupted(trace);
            }
            
            // Parse the output
            logger.debug("Parsing LLM output for step {}", stepCount);
            com.lyra.agent.agent.ParsedAction action = ReActOutputParser.parse(output);

            if (action.isFinalAnswer()) {
                logger.info("Final answer reached at step {}, answer: {}", stepCount, action.getFinalAnswer().substring(0, Math.min(100, action.getFinalAnswer().length())) + (action.getFinalAnswer().length() > 100 ? "..." : ""));
                
                // Add the final thought to messages
                if (action.getThought() != null && !action.getThought().isEmpty()) {
                    messages.add(Message.assistant(action.getThought()));
                }
                
                Trace finalTrace = stepTrace.end("completed", "Final answer reached");
                trace.add(finalTrace);
                
                logger.debug("Publishing agent finish event");
                eventBus.publish(new AgentEvent("agent.finish", Map.of("answer", action.getFinalAnswer(), "steps", stepCount)));
                return ModeResult.finalAnswer(action.getFinalAnswer(), trace);
            }

            // Process the action
            logger.debug("Processing action: {} with input: {}", action.getActionName(), action.getActionInput());
            eventBus.publish(new AgentEvent("agent.thought", Map.of("thought", action.getThought())));

            ToolExecutor tool = toolRegistry.getTool(action.getActionName());
            if (tool == null) {
                String errorMsg = "Error: Tool " + action.getActionName() + " not found";
                logger.warn(errorMsg);
                messages.add(Message.assistant(action.getThought()));
                messages.add(Message.tool(action.getActionName(), errorMsg));
                
                Trace errorTrace = stepTrace.end("error", errorMsg);
                trace.add(errorTrace);
                
                eventBus.publish(new AgentEvent("agent.error", Map.of("message", errorMsg)));
                continue;
            }

            try {
                logger.info("Executing tool: {} with input: {}", action.getActionName(), action.getActionInput());
                eventBus.publish(new AgentEvent("agent.tool.start", 
                    Map.of("name", action.getActionName(), "input", action.getActionInput())));

                Object result = tool.execute(action.getActionInput());
                String resultStr = result.toString();
                logger.info("Tool execution completed, result: {}", resultStr.substring(0, Math.min(100, resultStr.length())) + (resultStr.length() > 100 ? "..." : ""));

                eventBus.publish(new AgentEvent("agent.tool.end", 
                    Map.of("name", action.getActionName(), "result", resultStr)));

                // Add to messages for next iteration
                messages.add(Message.assistant(action.getThought()));
                messages.add(Message.tool(action.getActionName(), resultStr));

                // Update memory with the interaction
                memory.remember(new com.lyra.agent.memory.MemoryRecord(
                    "thought-" + step, action.getThought(), "thought", Instant.now(), Map.of("step", step)));
                memory.remember(new com.lyra.agent.memory.MemoryRecord(
                    "action-" + step, action.getActionName() + ": " + action.getActionInput(), "action", Instant.now(), Map.of("step", step)));
                memory.remember(new com.lyra.agent.memory.MemoryRecord(
                    "observation-" + step, resultStr, "observation", Instant.now(), Map.of("step", step)));

            } catch (Exception e) {
                String errorMsg = "Tool execution error: " + e.getMessage();
                logger.error("Tool execution failed: {}", errorMsg, e);
                messages.add(Message.assistant(action.getThought()));
                messages.add(Message.tool(action.getActionName(), errorMsg));
                
                Trace errorTrace = stepTrace.end("error", errorMsg);
                trace.add(errorTrace);
                
                eventBus.publish(new AgentEvent("agent.error", Map.of("message", errorMsg)));
                
                // Return an interrupted result with the error trace
                return ModeResult.interrupted(trace);
            }
        }

        logger.warn("Max steps ({}) reached without final answer", maxSteps);
        Trace timeoutTrace = Trace.start("timeout", maxSteps, name(), "default", "timeout", 
            "Agent failed to reach a final answer within " + maxSteps + " steps").end("interrupted", "Max steps reached");
        trace.add(timeoutTrace);
        
        eventBus.publish(new AgentEvent("agent.timeout", Map.of("maxSteps", maxSteps)));
        return ModeResult.interrupted(trace);
    }

    /**
     * Builds the prompt for the LLM based on the current state.
     *
     * @param messages The current conversation messages
     * @return The formatted prompt string
     */
    private String buildPrompt(List<Message> messages, AgentMemory agentMemory) {
        try {
            String template = StreamUtils.copyToString(promptTemplate.getInputStream(), StandardCharsets.UTF_8);
            String toolDesc = toolRegistry.getToolDescriptors().stream()
                    .map(d -> d.name + ": " + d.description)
                    .collect(Collectors.joining("\n"));
            String toolNames = toolRegistry.getToolDescriptors().stream()
                    .map(d -> d.name)
                    .collect(Collectors.joining(", "));

            // Get user question from the messages
            String userQuestion = messages.stream()
                    .filter(m -> m.getRole() == Message.Role.USER)
                    .findFirst()
                    .map(Message::getContent)
                    .orElse("No question provided");

            // Build history string
            StringBuilder history = new StringBuilder();
            for (Message msg : messages) {
                history.append(msg.getRole()).append(": ").append(msg.getContent()).append("\n");
            }

            return template.replace("{{tool_descriptions}}", toolDesc)
                    .replace("{{tool_names}}", toolNames)
                    .replace("{{user_question}}", userQuestion)
                    .replace("{{history}}", history.toString());
        } catch (Exception e) {
            return "Error reading prompt template: " + e.getMessage();
        }
    }
}