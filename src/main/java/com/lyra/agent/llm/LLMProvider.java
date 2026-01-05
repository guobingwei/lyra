package com.lyra.agent.llm;

/**
 * LLM 提供者接口。
 * 统一对大模型的调用入口。
 */
public interface LLMProvider {
    /**
     * 根据给定 Prompt 生成文本响应。
     *
     * @param prompt 输入提示
     * @return 模型返回的文本
     */
    String generate(String prompt);
}