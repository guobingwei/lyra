package com.lyra.agent.agent;

/**
 * Represents a thinking step in the agent's reasoning process.
 */
public class Thought {
    private final String content;
    private final int step;

    public Thought(String content, int step) {
        this.content = content;
        this.step = step;
    }

    public String getContent() {
        return content;
    }

    public int getStep() {
        return step;
    }
}