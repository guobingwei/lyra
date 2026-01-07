package com.lyra.agent.agent;

import com.lyra.agent.agent.Message;

import java.util.List;

/**
 * Manager for handling multiple agents, their lifecycle, and execution.
 */
public interface AgentManager {
    /**
     * Get an agent by its ID.
     * @param id the agent ID
     * @return the agent instance, or null if not found
     */
    Agent getAgent(String id);

    /**
     * Create a new agent with the specified profile.
     * @param profile the agent profile
     * @return the created agent
     */
    Agent createAgent(AgentProfile profile);

    /**
     * Run an agent with the given input message.
     * @param agent the agent to run
     * @param input the input message
     * @return the result of agent execution
     */
    ModeResult run(Agent agent, Message input);

    /**
     * Run an agent with the given input message and context.
     * @param agent the agent to run
     * @param messages the list of messages in the conversation
     * @return the result of agent execution
     */
    ModeResult run(Agent agent, List<Message> messages);

    /**
     * Get the default agent for a specific mode.
     * @param mode the mode name
     * @return the default agent for the mode
     */
    Agent defaultAgent(String mode);
}