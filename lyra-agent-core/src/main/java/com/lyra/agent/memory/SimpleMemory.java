package com.lyra.agent.memory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 基础内存实现，使用列表以文本形式串联历史。
 */
public class SimpleMemory implements Memory {
    private final List<String> history = new ArrayList<>();
    private String user;

    public void addUserMessage(String content) {
        user = content;
        history.add("Question: " + content);
    }

    public void addThought(String content) {
        if (content != null) {
            history.add("Thought: " + content);
        }
    }

    public void addAction(String name, Map<String, Object> input) {
        history.add("Action: " + name + " Input: " + String.valueOf(input));
    }

    public void addObservation(String content) {
        history.add("Observation: " + content);
    }

    public String getUserQuery() {
        return user;
    }

    public String getHistoryAsString() {
        return String.join("\n", history);
    }
}