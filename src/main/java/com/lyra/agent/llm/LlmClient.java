package com.lyra.agent.llm;

import com.lyra.agent.agent.Message;

import java.util.List;
import java.util.Map;

/**
 * LLM client interface that abstracts calls to large language models.
 * Supports both synchronous and streaming chat, as well as function/tool calling.
 */
public interface LlmClient {
    /**
     * Send a chat request to the LLM and get a response.
     * @param messages list of messages in the conversation
     * @param options additional options for the LLM call
     * @return the LLM response
     */
    LlmResponse chat(List<Message> messages, Map<String, Object> options);

    /**
     * Send a chat request to the LLM and get a streaming response.
     * @param messages list of messages in the conversation
     * @param options additional options for the LLM call
     * @return a stream of response chunks
     */
    // Note: We'll use a simple return here since Java doesn't have built-in streaming
    // In a real implementation, this would return a Stream<LlmResponseChunk> or Publisher
    default Object streamChat(List<Message> messages, Map<String, Object> options) {
        throw new UnsupportedOperationException("Streaming not implemented");
    }

    /**
     * Get the model name being used by this client.
     * @return the model name
     */
    String getModelName();
}