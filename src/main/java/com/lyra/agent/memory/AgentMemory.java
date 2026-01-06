package com.lyra.agent.memory;

import java.util.List;

/**
 * Agent memory interface for managing memory records.
 * Supports both short-term (working memory) and long-term memory.
 */
public interface AgentMemory {
    /**
     * Store a memory record.
     * @param record the memory record to store
     */
    void remember(MemoryRecord record);

    /**
     * Retrieve memory records based on a query.
     * @param query the memory query
     * @return list of matching memory records
     */
    List<MemoryRecord> recall(MemoryQuery query);

    /**
     * Clear all memory records.
     */
    void clear();

    /**
     * Get all memory records.
     * @return list of all memory records
     */
    List<MemoryRecord> getAll();
}