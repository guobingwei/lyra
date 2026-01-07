package com.lyra.agent.tool;

import java.util.Map;

/**
 * Request to execute a tool with specific parameters.
 */
public class ToolRequest {
    private final Map<String, Object> arguments;

    public ToolRequest(Map<String, Object> arguments) {
        this.arguments = arguments;
    }

    public Map<String, Object> getArguments() {
        return arguments;
    }
}