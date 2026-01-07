package com.lyra.agent.event;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 简单的内存事件总线实现。
 * 注意：当前实现为同步调用，生产环境可扩展为异步或基于消息队列。
 */
public class SimpleEventBus implements EventBus {
    private final Map<String, List<EventListener>> listeners = new ConcurrentHashMap<>();

    @Override
    public void publish(Event event) {
        List<EventListener> topicListeners = listeners.get(event.type());
        if (topicListeners != null) {
            // 为避免并发修改异常，复制一份列表进行遍历
            new ArrayList<>(topicListeners).forEach(listener -> listener.onEvent(event));
        }
    }

    @Override
    public void subscribe(String topic, EventListener listener) {
        listeners.computeIfAbsent(topic, k -> new ArrayList<>()).add(listener);
    }
}
