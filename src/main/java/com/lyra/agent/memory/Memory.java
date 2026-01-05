package com.lyra.agent.memory;

import java.util.Map;

/**
 * 会话记忆接口。
 * 负责记录用户问题、推理链与工具执行观测，并以文本历史形式输出。
 */
public interface Memory {
    /**
     * 记录用户问题
     */
    void addUserMessage(String content);

    /**
     * 记录当前思考
     */
    void addThought(String content);

    /**
     * 记录动作与输入
     */
    void addAction(String name, Map<String, Object> input);

    /**
     * 记录工具执行的观测结果
     */
    void addObservation(String content);

    /**
     * 获取用户原始问题
     */
    String getUserQuery();

    /**
     * 获取串联后的历史文本
     */
    String getHistoryAsString();
}