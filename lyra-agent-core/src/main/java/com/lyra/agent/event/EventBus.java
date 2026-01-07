package com.lyra.agent.event;

/**
 * 事件总线接口。
 * 支持发布订阅模式，用于模块间解耦与可观测性数据流转。
 */
public interface EventBus {
    /**
     * 发布事件。
     *
     * @param event 事件对象
     */
    void publish(Event event);

    /**
     * 订阅特定主题的事件。
     *
     * @param topic    主题（事件类型）
     * @param listener 监听器
     */
    void subscribe(String topic, EventListener listener);
}
