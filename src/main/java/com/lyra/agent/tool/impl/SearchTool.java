package com.lyra.agent.tool.impl;

import com.lyra.agent.tool.ToolExecutor;
import com.lyra.agent.tool.annotation.Tool;

import java.util.Map;

/**
 * 示例搜索工具。
 * 实际应用可接入外部搜索 API，此处返回占位结果。
 */
@Tool(name = "search", description = "Search the web for current information")
public class SearchTool implements ToolExecutor {
    /**
     * 执行搜索逻辑。
     *
     * @param args 参数，包含 {@code query}
     * @return 搜索结果占位文本
     */
    public Object execute(Map<String, Object> args) {
        Object q = args.get("query");
        return "Result for " + (q == null ? "" : q.toString());
    }

    /**
     * 返回参数 Schema 描述。
     *
     * @return JSON Schema 映射
     */
    public Map<String, Object> getParametersSchema() {
        java.util.Map<String, Object> param = new java.util.HashMap<>();
        param.put("type", "string");
        java.util.Map<String, Object> schema = new java.util.HashMap<>();
        schema.put("query", param);
        return schema;
    }
}