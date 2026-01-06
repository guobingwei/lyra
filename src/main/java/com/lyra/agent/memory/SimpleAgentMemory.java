package com.lyra.agent.memory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory implementation of AgentMemory.
 */
public class SimpleAgentMemory implements AgentMemory {
    private final List<MemoryRecord> records;
    private final Map<String, Object> metadata;

    public SimpleAgentMemory() {
        this.records = new ArrayList<>();
        this.metadata = new ConcurrentHashMap<>();
    }

    @Override
    public void remember(MemoryRecord record) {
        records.add(record);
    }

    @Override
    public List<MemoryRecord> recall(MemoryQuery query) {
        // Simple implementation that returns records matching the query text
        // In a real implementation, this would involve vector similarity search
        List<MemoryRecord> results = new ArrayList<>();
        
        for (MemoryRecord record : records) {
            if (query.getType() != null && !query.getType().equals(record.getType())) {
                continue;
            }
            
            if (record.getContent().toLowerCase().contains(query.getQueryText().toLowerCase())) {
                results.add(record);
                if (results.size() >= query.getLimit()) {
                    break;
                }
            }
        }
        
        return results;
    }

    @Override
    public void clear() {
        records.clear();
    }

    @Override
    public List<MemoryRecord> getAll() {
        return new ArrayList<>(records);
    }
}