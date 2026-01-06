package com.lyra.agent.agent;

import com.lyra.agent.event.AgentEvent;
import com.lyra.agent.event.EventBus;
import com.lyra.agent.llm.LLMProvider;
import com.lyra.agent.memory.SimpleAgentMemory;
import com.lyra.agent.tool.ToolRegistry;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple implementation of AgentManager.
 */
public class SimpleAgentManager implements AgentManager {
    private final ModeRegistry modeRegistry;
    private final LLMProvider llmProvider;
    private final ToolRegistry toolRegistry;
    private final EventBus eventBus;
    private final Map<String, Agent> agents;

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
        eventBus.publish(new AgentEvent("agent.start", Map.of("query", messages.stream()
            .filter(m -> m.getRole() == Message.Role.USER)
            .findFirst()
            .map(Message::getContent)
            .orElse(""))));

        ModeResult result = agent.mode().run(context);

        // Publish finish event if it's a final result
        if (result.isFinal()) {
            eventBus.publish(new AgentEvent("agent.finish", Map.of("answer", result.getFinalAnswer())));
        }

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