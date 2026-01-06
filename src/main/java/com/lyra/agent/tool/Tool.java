package com.lyra.agent.tool;

import java.util.Map;

/**
 * Tool interface representing an executable tool for agents.
 */
public interface Tool {
    /**
     * Get the name of the tool.
     * @return tool name
     */
    String name();

    /**
     * Get the schema describing the tool's parameters.
     * @return tool schema
     */
    ToolSchema schema();

    /**
     * Invoke the tool with the given request.
     * @param request tool request containing parameters
     * @return tool execution result
     */
    ToolResult invoke(ToolRequest request);
}