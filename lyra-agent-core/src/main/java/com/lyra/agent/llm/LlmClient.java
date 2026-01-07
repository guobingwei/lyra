package com.lyra.agent.llm;

import com.lyra.agent.agent.Message;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

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
     * @param chunkConsumer consumer that will receive each chunk of the response
     */
    default void streamChat(List<Message> messages, Map<String, Object> options, Consumer<StreamChunk> chunkConsumer) {
        throw new UnsupportedOperationException("Streaming not implemented");
    }

    /**
     * Get the model name being used by this client.
     * @return the model name
     */
    String getModelName();
}