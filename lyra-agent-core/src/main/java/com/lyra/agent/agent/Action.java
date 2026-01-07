package com.lyra.agent.agent;

import java.util.Map;

/**
 * Represents an action to be taken by the agent, typically invoking a tool.
 */
public class Action {
    private final String name;
    private final Map<String, Object> parameters;
    private final int step;

    public Action(String name, Map<String, Object> parameters, int step) {
        this.name = name;
        this.parameters = parameters;
        this.step = step;
    }

    public String getName() {
        return name;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public int getStep() {
        return step;
    }
}