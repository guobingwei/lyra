package com.lyra.agent.agent;

import com.lyra.agent.event.AgentEvent;
import com.lyra.agent.event.EventBus;
import com.lyra.agent.llm.LLMProvider;
import com.lyra.agent.memory.SimpleAgentMemory;
import com.lyra.agent.tool.ToolRegistry;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple implementation of AgentManager.
 */
public class SimpleAgentManager implements AgentManager {
    private final ModeRegistry modeRegistry;
    private final LLMProvider llmProvider;
    private final ToolRegistry toolRegistry;
    private final EventBus eventBus;
    private final Map<String, Agent> agents;
    private static final Logger logger = LoggerFactory.getLogger(SimpleAgentManager.class);

    public SimpleAgentManager(ModeRegistry modeRegistry, LLMProvider llmProvider, ToolRegistry toolRegistry, EventBus eventBus) {
        this.modeRegistry = modeRegistry;
        this.llmProvider = llmProvider;
        this.toolRegistry = toolRegistry;
        this.eventBus = eventBus;
        this.agents = new ConcurrentHashMap<>();
    }

    @Override
    public Agent getAgent(String id) {
        return agents.get(id);
    }

    @Override
    public Agent createAgent(AgentProfile profile) {
        Agent agent = new SimpleAgent(profile, new SimpleAgentMemory(), modeRegistry.get(profile.getMode()));
        agents.put(agent.id(), agent);
        return agent;
    }

    @Override
    public ModeResult run(Agent agent, Message input) {
        // Create initial context with a single user message
        List<Message> messages = List.of(input);
        return run(agent, messages);
    }

    @Override
    public ModeResult run(Agent agent, List<Message> messages) {
        logger.info("Starting agent execution, agent ID: {}, message count: {}", agent.id(), messages.size());
        
        // Create mode context with default options and empty trace
        ModeContext context = new ModeContext(
            messages,
            agent.memory(),
            agent.tools() != null ? null : toolRegistry, // Using global tool registry for now
            llmProvider,
            Map.of(),
            List.of() // Empty initial trace
        );

        // Publish start event
        logger.debug("Publishing agent start event");
        eventBus.publish(new AgentEvent("agent.start", Map.of("query", messages.stream()
            .filter(m -> m.getRole() == Message.Role.USER)
            .findFirst()
            .map(Message::getContent)
            .orElse(""))));

        logger.info("Executing agent mode: {}", agent.mode().name());
        ModeResult result = agent.mode().run(context);
        logger.info("Agent mode execution completed, result is final: {}", result.isFinal());

        // Publish finish event if it's a final result
        if (result.isFinal()) {
            logger.info("Publishing agent finish event");
            eventBus.publish(new AgentEvent("agent.finish", Map.of("answer", result.getFinalAnswer())));
        } else if (result.isInterrupted()) {
            logger.info("Publishing agent interrupted event");
            String errorMsg = "Agent execution was interrupted - possibly due to errors or reaching maximum steps";
            // If there are traces and the last trace has an error message, use that
            if (result.getTrace() != null && !result.getTrace().isEmpty()) {
                Trace lastTrace = result.getTrace().get(result.getTrace().size() - 1);
                logger.info("Last trace status: {}, details: {}", lastTrace.getStatus(), lastTrace.getDetails());
                if (lastTrace.getStatus().equals("error")) {
                    errorMsg = lastTrace.getDetails();
                }
            }
            logger.info("Publishing error event with message: {}", errorMsg.substring(0, Math.min(100, errorMsg.length())));
            eventBus.publish(new AgentEvent("agent.error", Map.of("message", errorMsg)));
        }

        logger.info("Returning result, final answer present: {}", result.getFinalAnswer() != null);
        return result;
    }

    @Override
    public Agent defaultAgent(String mode) {
        // Create a default agent profile
        AgentProfile profile = new AgentProfile(
            "default-agent",
            "Default agent for " + mode + " mode",
            Map.of(),
            mode
        );
        
        return createAgent(profile);
    }
}