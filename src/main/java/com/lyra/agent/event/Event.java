package com.lyra.agent.event;

import java.time.Instant;
import java.util.Map;

/**
 * 基础事件接口。
 */
public interface Event {
    /**
     * 事件类型（主题）。
     */
    String type();

    /**
     * 事件发生时间。
     */
    Instant timestamp();

    /**
     * 事件载荷数据。
     */
    Map<String, Object> payload();
}
