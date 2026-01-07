package com.lyra.agent.agent;

import java.time.Instant;

/**
 * Execution trace containing observability data for agent execution.
 * Includes timestamps, step information, duration, and status.
 */
public class Trace {
    private final String id;
    private final int step;
    private final String mode;
    private final String agentId;
    private final String action;
    private final String details;
    private final Instant startTime;
    private final Instant endTime;
    private final long durationMs;
    private final String status; // e.g., "success", "error", "interrupted"

    public Trace(String id, int step, String mode, String agentId, String action, String details, 
                 Instant startTime, Instant endTime, long durationMs, String status) {
        this.id = id;
        this.step = step;
        this.mode = mode;
        this.agentId = agentId;
        this.action = action;
        this.details = details;
        this.startTime = startTime;
        this.endTime = endTime;
        this.durationMs = durationMs;
        this.status = status;
    }

    public static Trace start(String id, int step, String mode, String agentId, String action, String details) {
        return new Trace(id, step, mode, agentId, action, details, Instant.now(), null, 0, "started");
    }

    public Trace end(String status, String details) {
        Instant now = Instant.now();
        long duration = now.toEpochMilli() - this.startTime.toEpochMilli();
        return new Trace(this.id, this.step, this.mode, this.agentId, this.action, 
                        details, this.startTime, now, duration, status);
    }

    public String getId() {
        return id;
    }

    public int getStep() {
        return step;
    }

    public String getMode() {
        return mode;
    }

    public String getAgentId() {
        return agentId;
    }

    public String getAction() {
        return action;
    }

    public String getDetails() {
        return details;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public String getStatus() {
        return status;
    }
}