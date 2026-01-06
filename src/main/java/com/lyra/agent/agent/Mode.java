package com.lyra.agent.agent;

import com.lyra.agent.llm.LLMProvider;
import com.lyra.agent.memory.AgentMemory;
import com.lyra.agent.tool.ToolRegistry;

import java.util.List;

/**
 * Agent mode interface that defines the strategy for agent reasoning and action.
 * Supports various patterns like ReAct, Plan-and-Execute, Tree-of-Thought, etc.
 */
public interface Mode {
    /**
     * Get the name of this mode.
     * @return mode name
     */
    String name();

    /**
     * Execute the mode with the given context.
     * @param context the mode execution context
     * @return the result of mode execution
     */
    ModeResult run(ModeContext context);
}