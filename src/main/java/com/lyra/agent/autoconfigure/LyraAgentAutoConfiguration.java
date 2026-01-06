package com.lyra.agent.autoconfigure;

import com.lyra.agent.agent.*;
import com.lyra.agent.event.EventBus;
import com.lyra.agent.event.SimpleEventBus;
import com.lyra.agent.llm.EmbeddingModel;
import com.lyra.agent.llm.LLMProvider;
import com.lyra.agent.llm.OpenAIEmbeddingModel;
import com.lyra.agent.llm.OpenAILLMProvider;
import com.lyra.agent.memory.InMemoryVectorStore;
import com.lyra.agent.memory.VectorStore;
import com.lyra.agent.tool.ToolExecutor;
import com.lyra.agent.tool.ToolRegistry;
import com.lyra.agent.web.AgentController;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.util.List;

@AutoConfiguration
@EnableConfigurationProperties(LyraAgentProperties.class)
@ConditionalOnProperty(prefix = "agent", name = "enabled", havingValue = "true", matchIfMissing = true)
/**
 * Lyra Agent 的 Spring Boot 自动配置。
 * 提供 LLMProvider、ToolRegistry、ReActAgent 与可选的 REST 控制器。
 */
public class LyraAgentAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    /**
     * 构建事件总线。
     *
     * @return EventBus 实例
     */
    public EventBus eventBus() {
        return new SimpleEventBus();
    }

    @Bean
    @ConditionalOnMissingBean
    /**
     * 构建 EmbeddingModel。
     *
     * @param props 配置属性
     * @return EmbeddingModel 实例
     */
    public EmbeddingModel embeddingModel(LyraAgentProperties props) {
        return new OpenAIEmbeddingModel(props.getLlm());
    }

    @Bean
    @ConditionalOnMissingBean
    /**
     * 构建向量存储。
     * 支持 in-memory 和 milvus。
     *
     * @param props 配置属性
     * @param embeddingModel 嵌入模型
     * @return VectorStore 实例
     */
    public VectorStore vectorStore(LyraAgentProperties props, EmbeddingModel embeddingModel) {
        if ("milvus".equalsIgnoreCase(props.getVector().getStore())) {
            return new com.lyra.agent.memory.MilvusVectorStore(embeddingModel, props.getVector().getMilvus());
        }
        return new InMemoryVectorStore(embeddingModel);
    }

    @Bean
    /**
     * 构建 LLMProvider。
     *
     * @param props 配置属性
     * @return LLMProvider 实例
     */
    public LLMProvider llmProvider(LyraAgentProperties props) {
        String provider = props.getLlm().getProvider();
        if ("gemini".equalsIgnoreCase(provider)) {
            return new com.lyra.agent.llm.GeminiLLMProvider(props.getLlm());
        } else if ("deepseek".equalsIgnoreCase(provider)) {
            return new com.lyra.agent.llm.DeepSeekLLMProvider(props.getLlm());
        } else if ("qwen".equalsIgnoreCase(provider)) {
            return new com.lyra.agent.llm.QwenLLMProvider(props.getLlm());
        }
        // Default to OpenAI
        return new OpenAILLMProvider(props.getLlm());
    }

    @Bean
    @ConditionalOnMissingBean
    /**
     * 默认的搜索工具 Bean。
     *
     * @return SearchTool 实例
     */
    public com.lyra.agent.tool.impl.SearchTool searchTool() {
        return new com.lyra.agent.tool.impl.SearchTool();
    }

    @Bean
    /**
     * 构建工具注册表，收集所有 ToolExecutor。
     *
     * @param executors 已注入的工具执行器列表
     * @return 工具注册表
     */
    public ToolRegistry toolRegistry(List<ToolExecutor> executors) {
        return new ToolRegistry(executors);
    }

    @Bean
    /**
     * 构建 ReactMode。
     *
     * @param llmProvider LLM 提供者
     * @param toolRegistry 工具注册表
     * @param eventBus 事件总线
     * @param props 属性配置
     * @param rl 资源加载器，用于读取 Prompt 模板
     * @return ReactMode 实例
     */
    public ReactMode reactMode(LLMProvider llmProvider, ToolRegistry toolRegistry, EventBus eventBus, LyraAgentProperties props, ResourceLoader rl) {
        Resource prompt = rl.getResource(props.getPromptPath());
        return new ReactMode(llmProvider, toolRegistry, eventBus, props.getMaxSteps(), prompt);
    }

    @Bean
    @ConditionalOnMissingBean
    /**
     * 构建 ModeRegistry。
     *
     * @param reactMode ReAct mode
     * @return ModeRegistry 实例
     */
    public ModeRegistry modeRegistry(ReactMode reactMode) {
        ModeRegistry registry = new SimpleModeRegistry();
        registry.register(reactMode);
        return registry;
    }

    @Bean
    @ConditionalOnMissingBean
    /**
     * 构建 AgentManager。
     *
     * @param modeRegistry Mode registry
     * @param llmProvider LLM provider
     * @param toolRegistry Tool registry
     * @param eventBus Event bus
     * @return AgentManager 实例
     */
    public AgentManager agentManager(ModeRegistry modeRegistry, LLMProvider llmProvider, ToolRegistry toolRegistry, EventBus eventBus) {
        return new SimpleAgentManager(modeRegistry, llmProvider, toolRegistry, eventBus);
    }

    @Bean
    @ConditionalOnProperty(prefix = "agent.api", name = "expose", havingValue = "true")
    /**
     * 可选暴露 REST 控制器。
     *
     * @param agent ReActAgent 实例
     * @return 控制器实例
     */
    public AgentController agentController(AgentManager agent) {
        return new AgentController(agent);
    }
}
