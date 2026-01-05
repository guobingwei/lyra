package com.lyra.agent.tool;

/**
 * 工具名称与描述的数据结构。
 */
public class ToolDescriptor {
    public final String name;
    public final String description;

    /**
     * 构造函数。
     *
     * @param name        工具名称
     * @param description 工具描述
     */
    public ToolDescriptor(String name, String description) {
        this.name = name;
        this.description = description;
    }
}