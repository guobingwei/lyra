package com.lyra.agent.agent;

import java.util.Map;

/**
 * 解析后的动作或最终答案的载体。
 * 当 {@code finalAnswer} 为真时，表示包含最终文本答案；否则包含下一步动作信息。
 */
public class ParsedAction {
    private final boolean finalAnswer;
    private final String finalText;
    private final String actionName;
    private final Map<String, Object> actionInput;
    private final String thought;

    /**
     * 构造一个非最终答案的动作。
     *
     * @param thought     当前思考
     * @param actionName  动作名称
     * @param actionInput 动作输入参数
     */
    public ParsedAction(String thought, String actionName, Map<String, Object> actionInput) {
        this.finalAnswer = false;
        this.finalText = null;
        this.thought = thought;
        this.actionName = actionName;
        this.actionInput = actionInput;
    }

    /**
     * 构造一个最终答案。
     *
     * @param finalText 最终答案文本
     */
    public ParsedAction(String finalText) {
        this.finalAnswer = true;
        this.finalText = finalText;
        this.thought = null;
        this.actionName = null;
        this.actionInput = null;
    }

    /**
     * 是否为最终答案
     */
    public boolean isFinalAnswer() {
        return finalAnswer;
    }

    /**
     * 获取最终答案文本
     */
    public String getFinalAnswer() {
        return finalText;
    }

    /**
     * 获取动作名称
     */
    public String getActionName() {
        return actionName;
    }

    /**
     * 获取动作输入参数
     */
    public Map<String, Object> getActionInput() {
        return actionInput;
    }

    /**
     * 获取当前思考文本
     */
    public String getThought() {
        return thought;
    }
}