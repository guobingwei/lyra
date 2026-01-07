package com.lyra.agent.agent;

import com.lyra.agent.event.AgentEvent;
import com.lyra.agent.event.EventBus;
import com.lyra.agent.llm.LLMProvider;
import com.lyra.agent.memory.Memory;
import com.lyra.agent.memory.SimpleMemory;
import com.lyra.agent.parser.ReActOutputParser;
import com.lyra.agent.tool.ToolExecutor;
import com.lyra.agent.tool.ToolRegistry;
import org.springframework.core.io.Resource;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ReAct 风格智能体的核心实现。
 * 根据 Prompt 模板与会话记忆构建输入，调用 LLM 获取响应，
 * 解析为动作并执行已注册的工具，在最大步数内循环直至产生最终答案。
 */
public class ReActAgent {
    private final LLMProvider llmProvider;
    private final ToolRegistry toolRegistry;
    private final EventBus eventBus;
    private final int maxSteps;
    private final Resource promptTemplate;

    /**
     * 构造函数。
     *
     * @param llmProvider    LLM 提供者
     * @param toolRegistry   工具注册表
     * @param eventBus       事件总线
     * @param maxSteps       最大推理步数
     * @param promptTemplate Prompt 模板资源
     */
    public ReActAgent(LLMProvider llmProvider, ToolRegistry toolRegistry, EventBus eventBus, int maxSteps, Resource promptTemplate) {
        this.llmProvider = llmProvider;
        this.toolRegistry = toolRegistry;
        this.eventBus = eventBus;
        this.maxSteps = maxSteps;
        this.promptTemplate = promptTemplate;
    }

    /**
     * 运行智能体主循环，基于用户问题驱动 ReAct 推理与工具调用。
     *
     * @param userQuery 用户问题
     * @return 最终答案或失败提示
     */
    public String run(String userQuery) {
        Memory memory = new SimpleMemory();
        memory.addUserMessage(userQuery);
        eventBus.publish(new AgentEvent("agent.start", Map.of("query", userQuery)));

        for (int step = 0; step < maxSteps; step++) {
            eventBus.publish(new AgentEvent("agent.step", Map.of("step", step + 1)));
            String prompt = buildPrompt(memory);
            String output = llmProvider.generate(prompt);
            com.lyra.agent.agent.ParsedAction action = ReActOutputParser.parse(output);
            
            if (action.isFinalAnswer()) {
                eventBus.publish(new AgentEvent("agent.finish", Map.of("answer", action.getFinalAnswer())));
                return action.getFinalAnswer();
            }
            
            eventBus.publish(new AgentEvent("agent.thought", Map.of("thought", action.getThought())));

            ToolExecutor tool = toolRegistry.getTool(action.getActionName());
            if (tool == null) {
                String errorMsg = "Error: Tool " + action.getActionName() + " not found";
                memory.addObservation(errorMsg);
                eventBus.publish(new AgentEvent("agent.error", Map.of("message", errorMsg)));
                continue;
            }
            
            try {
                eventBus.publish(new AgentEvent("agent.tool.start", Map.of("name", action.getActionName(), "input", action.getActionInput())));
                Object result = tool.execute(action.getActionInput());
                String resultStr = result.toString();
                eventBus.publish(new AgentEvent("agent.tool.end", Map.of("name", action.getActionName(), "result", resultStr)));
                
                memory.addThought(action.getThought());
                memory.addAction(action.getActionName(), action.getActionInput());
                memory.addObservation(resultStr);
            } catch (Exception e) {
                String errorMsg = "Tool execution error: " + e.getMessage();
                memory.addObservation(errorMsg);
                eventBus.publish(new AgentEvent("agent.error", Map.of("message", errorMsg)));
            }
        }
        
        eventBus.publish(new AgentEvent("agent.timeout", Map.of("maxSteps", maxSteps)));
        return "Agent failed to reach a final answer within " + maxSteps + " steps.";
    }

    /**
     * 基于模板渲染当前工具描述与推理历史，生成 LLM 输入。
     *
     * @param memory 会话记忆
     * @return 生成的 Prompt 字符串
     */
    private String buildPrompt(Memory memory) {
        String template = readResource(promptTemplate);
        String toolDesc = toolRegistry.getToolDescriptors().stream()
                .map(d -> d.name + ": " + d.description)
                .collect(Collectors.joining("\n"));
        String toolNames = toolRegistry.getToolDescriptors().stream()
                .map(d -> d.name)
                .collect(Collectors.joining(", "));
        String history = memory.getHistoryAsString();
        return template.replace("{{tool_descriptions}}", toolDesc)
                .replace("{{tool_names}}", toolNames)
                .replace("{{user_question}}", memory.getUserQuery())
                .replace("{{history}}", history);
    }

    /**
     * 安全读取资源为字符串。
     *
     * @param resource 资源对象
     * @return 读取到的文本，失败返回空字符串
     */
    private String readResource(Resource resource) {
        try {
            return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }
}