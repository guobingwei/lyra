package com.lyra.agent.tool;

import com.lyra.agent.tool.annotation.Tool;

import java.util.*;

/**
 * 工具注册与查询中心。
 * 扫描带有 {@link com.lyra.agent.tool.annotation.Tool} 注解的执行器并注册，
 * 提供按名称获取工具与导出工具描述列表的能力。
 */
public class ToolRegistry {
    private final Map<String, ToolExecutor> tools = new HashMap<>();
    private final List<ToolDescriptor> descriptors = new ArrayList<>();

    public ToolRegistry(List<ToolExecutor> executors) {
        for (ToolExecutor e : executors) {
            Tool ann = e.getClass().getAnnotation(Tool.class);
            if (ann != null) {
                tools.put(ann.name(), e);
                descriptors.add(new ToolDescriptor(ann.name(), ann.description()));
            }
        }
    }

    /**
     * 根据工具名获取执行器。
     *
     * @param name 工具名称
     * @return 对应的工具执行器，未找到返回 null
     */
    public ToolExecutor getTool(String name) {
        return tools.get(name);
    }

    /**
     * 获取只读的工具描述列表。
     *
     * @return 工具名称与描述的列表（不可修改）
     */
    public List<ToolDescriptor> getToolDescriptors() {
        return Collections.unmodifiableList(descriptors);
    }
}