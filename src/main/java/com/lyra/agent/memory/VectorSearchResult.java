package com.lyra.agent.memory;

import java.util.Map;

/**
 * 向量搜索结果。
 */
public class VectorSearchResult {
    private final String id;
    private final String text;
    private final Map<String, Object> metadata;
    private final double score;

    public VectorSearchResult(String id, String text, Map<String, Object> metadata, double score) {
        this.id = id;
        this.text = text;
        this.metadata = metadata;
        this.score = score;
    }

    public String getId() {
        return id;
    }

    public String getText() {
        return text;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public double getScore() {
        return score;
    }
}
