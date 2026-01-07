package com.lyra.agent.agent;

import java.time.Instant;
import java.util.Map;

/**
 * Message in a conversation with role-based structure.
 * Supports system, user, assistant, and tool roles.
 */
public class Message {
    public enum Role {
        SYSTEM, USER, ASSISTANT, TOOL
    }

    private final Role role;
    private final String content;
    private final String name; // Optional name for the message author
    private final Instant timestamp;
    private final Map<String, Object> metadata;

    public Message(Role role, String content, String name, Instant timestamp, Map<String, Object> metadata) {
        this.role = role;
        this.content = content;
        this.name = name;
        this.timestamp = timestamp;
        this.metadata = metadata;
    }

    public static Message system(String content) {
        return new Message(Role.SYSTEM, content, null, Instant.now(), Map.of());
    }

    public static Message user(String content) {
        return new Message(Role.USER, content, null, Instant.now(), Map.of());
    }

    public static Message assistant(String content) {
        return new Message(Role.ASSISTANT, content, null, Instant.now(), Map.of());
    }

    public static Message tool(String toolName, String content) {
        return new Message(Role.TOOL, content, toolName, Instant.now(), Map.of());
    }

    public Role getRole() {
        return role;
    }

    public String getContent() {
        return content;
    }

    public String getName() {
        return name;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }
}