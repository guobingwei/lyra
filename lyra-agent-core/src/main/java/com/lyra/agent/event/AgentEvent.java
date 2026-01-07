package com.lyra.agent.event;

import java.time.Instant;
import java.util.Map;

/**
 * 通用 Agent 事件实现。
 */
public class AgentEvent implements Event {
    private final String type;
    private final Instant timestamp;
    private final Map<String, Object> payload;

    public AgentEvent(String type, Map<String, Object> payload) {
        this.type = type;
        this.timestamp = Instant.now();
        this.payload = payload;
    }

    @Override
    public String type() {
        return type;
    }

    @Override
    public Instant timestamp() {
        return timestamp;
    }

    @Override
    public Map<String, Object> payload() {
        return payload;
    }
}
