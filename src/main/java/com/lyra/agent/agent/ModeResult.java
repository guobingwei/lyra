package com.lyra.agent.agent;

import java.util.List;

/**
 * Result of mode execution containing the final answer and execution trace.
 */
public class ModeResult {
    private final String finalAnswer;
    private final List<Trace> trace;
    private final boolean isInterrupted;

    /**
     * Create a final answer result.
     * @param finalAnswer the final answer
     * @param trace execution trace
     * @return ModeResult instance
     */
    public static ModeResult finalAnswer(String finalAnswer, List<Trace> trace) {
        return new ModeResult(finalAnswer, trace, false);
    }

    /**
     * Create an interrupted result when execution is stopped before completion.
     * @param trace execution trace
     * @return ModeResult instance
     */
    public static ModeResult interrupted(List<Trace> trace) {
        return new ModeResult(null, trace, true);
    }

    private ModeResult(String finalAnswer, List<Trace> trace, boolean isInterrupted) {
        this.finalAnswer = finalAnswer;
        this.trace = trace;
        this.isInterrupted = isInterrupted;
    }

    public String getFinalAnswer() {
        return finalAnswer;
    }

    public List<Trace> getTrace() {
        return trace;
    }

    public boolean isInterrupted() {
        return isInterrupted;
    }

    public boolean isFinal() {
        return !isInterrupted && finalAnswer != null;
    }
}