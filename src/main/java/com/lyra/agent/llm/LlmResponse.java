package com.lyra.agent.llm;

import java.util.Map;

/**
 * Response from the LLM containing the content and additional metadata.
 */
public class LlmResponse {
    private final String content;
    private final Map<String, Object> metadata;
    private final String finishReason;

    public LlmResponse(String content, Map<String, Object> metadata, String finishReason) {
        this.content = content;
        this.metadata = metadata;
        this.finishReason = finishReason;
    }

    public String getContent() {
        return content;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public String getFinishReason() {
        return finishReason;
    }
}