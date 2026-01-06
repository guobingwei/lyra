package com.lyra.agent.memory;

import java.time.Instant;
import java.util.Map;

/**
 * A record in agent memory containing content, metadata, and timestamp.
 */
public class MemoryRecord {
    private final String id;
    private final String content;
    private final String type; // e.g., "observation", "thought", "action", "user_input"
    private final Instant timestamp;
    private final Map<String, Object> metadata;

    public MemoryRecord(String id, String content, String type, Instant timestamp, Map<String, Object> metadata) {
        this.id = id;
        this.content = content;
        this.type = type;
        this.timestamp = timestamp;
        this.metadata = metadata;
    }

    public String getId() {
        return id;
    }

    public String getContent() {
        return content;
    }

    public String getType() {
        return type;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }
}