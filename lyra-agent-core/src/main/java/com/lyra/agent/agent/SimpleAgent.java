package com.lyra.agent.agent;

import com.lyra.agent.memory.AgentMemory;
import com.lyra.agent.tool.Tool;

import java.util.List;

/**
 * Simple implementation of the Agent interface.
 */
public class SimpleAgent implements Agent {
    private final AgentProfile profile;
    private final AgentMemory memory;
    private final Mode mode;

    public SimpleAgent(AgentProfile profile, AgentMemory memory, Mode mode) {
        this.profile = profile;
        this.memory = memory;
        this.mode = mode;
    }

    @Override
    public String id() {
        return profile.getName();
    }

    @Override
    public AgentProfile profile() {
        return profile;
    }

    @Override
    public AgentMemory memory() {
        return memory;
    }

    @Override
    public List<Tool> tools() {
        // For now, return an empty list. In a real implementation, this would come from the mode or agent configuration
        return List.of();
    }

    @Override
    public Mode mode() {
        return mode;
    }
}