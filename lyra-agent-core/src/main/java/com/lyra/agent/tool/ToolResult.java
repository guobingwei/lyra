package com.lyra.agent.tool;

import java.util.Map;

/**
 * Result of executing a tool.
 */
public class ToolResult {
    private final String output;
    private final boolean success;
    private final String error;
    private final Map<String, Object> metadata;

    public ToolResult(String output, boolean success, String error, Map<String, Object> metadata) {
        this.output = output;
        this.success = success;
        this.error = error;
        this.metadata = metadata;
    }

    public static ToolResult success(String output) {
        return new ToolResult(output, true, null, Map.of());
    }

    public static ToolResult failure(String error) {
        return new ToolResult(null, false, error, Map.of());
    }

    public String getOutput() {
        return output;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getError() {
        return error;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }
}