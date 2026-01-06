package com.lyra.agent.llm;

import com.lyra.agent.agent.Message;

import java.util.List;
import java.util.Map;

/**
 * LLM 提供者接口。
 * 统一对大模型的调用入口。
 */
public interface LLMProvider extends LlmClient {
    /**
     * 根据给定 Prompt 生成文本响应。
     *
     * @param prompt 输入提示
     * @return 模型返回的文本
     */
    String generate(String prompt);

    /**
     * Send a chat request to the LLM and get a response.
     * This method is inherited from LlmClient.
     * @param messages list of messages in the conversation
     * @param options additional options for the LLM call
     * @return the LLM response
     */
    @Override
    default LlmResponse chat(List<Message> messages, Map<String, Object> options) {
        // Default implementation that converts messages to a prompt string
        // This is a simple implementation that concatenates all messages
        StringBuilder prompt = new StringBuilder();
        for (Message msg : messages) {
            prompt.append(msg.getRole().name()).append(": ").append(msg.getContent()).append("\n");
        }
        String response = generate(prompt.toString());
        return new LlmResponse(response, Map.of(), "stop");
    }

    /**
     * Get the model name being used by this client.
     * This method is inherited from LlmClient.
     * @return the model name
     */
    @Override
    default String getModelName() {
        return "default-model";
    }
}