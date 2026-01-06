package com.lyra.agent.agent;

import com.lyra.agent.llm.LLMProvider;
import com.lyra.agent.memory.AgentMemory;
import com.lyra.agent.tool.ToolRegistry;

import java.util.List;
import java.util.Map;

/**
 * Context for mode execution containing all necessary components and state.
 */
public class ModeContext {
    private final List<Message> messages;
    private final AgentMemory memory;
    private final ToolRegistry toolRegistry;
    private final LLMProvider llmProvider;
    private final Map<String, Object> options;
    private final List<Trace> trace;

    public ModeContext(List<Message> messages, AgentMemory memory, ToolRegistry toolRegistry, 
                      LLMProvider llmProvider, Map<String, Object> options, List<Trace> trace) {
        this.messages = messages;
        this.memory = memory;
        this.toolRegistry = toolRegistry;
        this.llmProvider = llmProvider;
        this.options = options;
        this.trace = trace;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public AgentMemory getMemory() {
        return memory;
    }

    public ToolRegistry getToolRegistry() {
        return toolRegistry;
    }

    public LLMProvider getLlmProvider() {
        return llmProvider;
    }

    public Map<String, Object> getOptions() {
        return options;
    }

    public List<Trace> getTrace() {
        return trace;
    }
}