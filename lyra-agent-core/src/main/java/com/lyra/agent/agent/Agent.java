package com.lyra.agent.agent;

import com.lyra.agent.memory.AgentMemory;
import com.lyra.agent.tool.Tool;

import java.util.List;

/**
 * Agent interface representing an intelligent agent entity.
 * Contains identity, capabilities, mode, memory, and tools.
 */
public interface Agent {
    /**
     * Get the unique identifier for this agent.
     * @return agent ID
     */
    String id();

    /**
     * Get the agent profile containing configuration and metadata.
     * @return agent profile
     */
    AgentProfile profile();

    /**
     * Get the agent's memory system.
     * @return agent memory
     */
    AgentMemory memory();

    /**
     * Get the list of tools available to this agent.
     * @return list of tools
     */
    List<Tool> tools();

    /**
     * Get the current mode of this agent.
     * @return agent mode
     */
    Mode mode();
}