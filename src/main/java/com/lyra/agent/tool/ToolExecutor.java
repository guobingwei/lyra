package com.lyra.agent.tool;

import java.util.Map;

/**
 * 外部工具执行器接口。
 * 为智能体提供可调用的能力模块及其参数描述。
 */
public interface ToolExecutor {
    /**
     * 执行工具逻辑。
     *
     * @param args 参数映射
     * @return 执行结果对象
     * @throws Exception 执行过程中的异常
     */
    Object execute(Map<String, Object> args) throws Exception;

    /**
     * 返回参数的 JSON Schema 描述。
     *
     * @return 参数 Schema 映射
     */
    Map<String, Object> getParametersSchema();
}