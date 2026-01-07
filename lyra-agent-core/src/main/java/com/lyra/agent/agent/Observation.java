package com.lyra.agent.agent;

/**
 * Represents the result of an action, typically the output from a tool execution.
 */
public class Observation {
    private final String content;
    private final String source; // The tool or action that produced this observation
    private final int step;

    public Observation(String content, String source, int step) {
        this.content = content;
        this.source = source;
        this.step = step;
    }

    public String getContent() {
        return content;
    }

    public String getSource() {
        return source;
    }

    public int getStep() {
        return step;
    }
}