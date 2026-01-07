package com.lyra.agent.memory;

import java.util.Map;

/**
 * Query object for retrieving memory records.
 */
public class MemoryQuery {
    private final String queryText;
    private final String type; // Optional: filter by memory type
    private final int limit; // Maximum number of records to return
    private final Map<String, Object> filters; // Additional filters as key-value pairs

    public MemoryQuery(String queryText, String type, int limit, Map<String, Object> filters) {
        this.queryText = queryText;
        this.type = type;
        this.limit = limit;
        this.filters = filters;
    }

    public MemoryQuery(String queryText) {
        this(queryText, null, 10, Map.of());
    }

    public String getQueryText() {
        return queryText;
    }

    public String getType() {
        return type;
    }

    public int getLimit() {
        return limit;
    }

    public Map<String, Object> getFilters() {
        return filters;
    }
}