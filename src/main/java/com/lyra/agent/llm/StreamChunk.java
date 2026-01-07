package com.lyra.agent.llm;

import java.util.Map;

/**
 * Represents a chunk of streaming response from LLM.
 */
public class StreamChunk {
    private final String content;
    private final boolean done;
    private final Map<String, Object> metadata;
    private final String finishReason;

    public StreamChunk(String content, boolean done, Map<String, Object> metadata, String finishReason) {
        this.content = content;
        this.done = done;
        this.metadata = metadata;
        this.finishReason = finishReason;
    }

    public String getContent() {
        return content;
    }

    public boolean isDone() {
        return done;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public String getFinishReason() {
        return finishReason;
    }
}
