package com.lyra.agent.tool;

import java.util.Map;

/**
 * Schema describing a tool's parameters and functionality.
 */
public class ToolSchema {
    private final String name;
    private final String description;
    private final Map<String, Object> parameters; // JSON Schema for parameters

    public ToolSchema(String name, String description, Map<String, Object> parameters) {
        this.name = name;
        this.description = description;
        this.parameters = parameters;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }
}