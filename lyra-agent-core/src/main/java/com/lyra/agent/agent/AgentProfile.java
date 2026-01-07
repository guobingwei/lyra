package com.lyra.agent.agent;

import java.util.Map;

/**
 * Agent profile containing configuration and metadata for an agent.
 */
public class AgentProfile {
    private final String name;
    private final String description;
    private final Map<String, Object> config;
    private final String mode;

    public AgentProfile(String name, String description, Map<String, Object> config, String mode) {
        this.name = name;
        this.description = description;
        this.config = config;
        this.mode = mode;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Map<String, Object> getConfig() {
        return config;
    }

    public String getMode() {
        return mode;
    }
}