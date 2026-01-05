# Java 智能体框架技术方案（Spring Boot 集成版）

## 目标与范围

- 提供可扩展的智能体框架，原生支持 ReAct 模式，并可扩展 Plan-and-Execute、Tree-of-Thought、Reflexion 等模式。
- 以 Spring Boot 为集成载体，提供自动装配、配置化管理、可插拔适配器，兼顾易用性与工程可维护性。
- 支持工具调用、记忆管理、LLM 接入、链路追踪、可观测性、错误处理与并发控制。

## 总体架构

- 核心层 `multiagent-core`：定义领域模型、接口契约、执行管线、事件总线、基础设施抽象。
- 模式层 `multiagent-modes-*`：每个模式单独模块，复用核心层的接口并实现策略。
- 适配层 `multiagent-adapters-*`：LLM、向量库、工具库、存储、消息总线等第三方适配。
- Spring 集成层 `multiagent-spring-boot-starter`：AutoConfiguration、Properties、Bean 定义、条件装配、默认实现。
- 示例层 `multiagent-examples`：演示如何在 Spring Boot 应用中使用框架运行智能体。

## 领域模型

- `Agent`：智能体实体，包含身份、能力、模式、内存、工具集合。
- `Mode`：智能体思维与行动的策略接口，支持 ReAct 等模式。
- `Message`：角色消息结构，支持系统、用户、助手、工具等角色。
- `Thought`：思考步骤产物。
- `Action`：对外工具或环境的调用请求。
- `Observation`：工具执行结果反馈。
- `Trace`：执行过程的可观测数据，含时间戳、步骤、耗时、状态。

## 核心接口

```java
public interface Agent {
  String id();
  AgentProfile profile();
  AgentMemory memory();
  List<Tool> tools();
  Mode mode();
}

public interface Mode {
  String name();
  ModeResult run(ModeContext context);
}

public interface Tool {
  String name();
  ToolSchema schema();
  ToolResult invoke(ToolRequest request);
}

public interface LlmClient {
  LlmResponse chat(List<Message> messages, LlmOptions options);
  Stream<LlmResponseChunk> streamChat(List<Message> messages, LlmOptions options);
}

public interface AgentMemory {
  void remember(MemoryRecord record);
  List<MemoryRecord> recall(MemoryQuery query);
}

public interface VectorStore {
  void upsert(String id, String text, Map<String, Object> metadata);
  List<VectorSearchResult> similaritySearch(String text, int k);
}

public interface EventBus {
  void publish(Event event);
  void subscribe(String topic, EventListener listener);
}
```

## ReAct 模式设计

- 思维-行动-观测循环，直到达到终止条件或达到步数上限。
- 使用 `PromptTemplate` 生成 ReAct 指令，包含思维轨迹占位符与工具列表说明。
- 支持工具函数调用式输出格式与自然语言解析两种路径。
- 支持流式输出，将思维与观测实时推送到事件总线与订阅方。

```java
public final class ReactMode implements Mode {
  private final LlmClient llm;
  private final ToolRegistry toolRegistry;
  private final PromptTemplate prompt;
  private final int maxSteps;

  public String name() { return "react"; }

  public ModeResult run(ModeContext ctx) {
    List<Message> history = new ArrayList<>(ctx.messages());
    for (int step = 0; step < maxSteps; step++) {
      Message system = prompt.render(ctx);
      LlmResponse resp = llm.chat(merge(history, system), ctx.options());
      ParsedOutput parsed = ReactParser.parse(resp.content());
      if (parsed.isFinal()) {
        return ModeResult.finalAnswer(parsed.finalAnswer(), ctx.trace());
      }
      Tool t = toolRegistry.get(parsed.toolName());
      ToolResult tr = t.invoke(parsed.toolRequest());
      history.add(Message.assistant(parsed.thought()));
      history.add(Message.tool(parsed.toolName(), tr.output()));
    }
    return ModeResult.interrupted(ctx.trace());
  }
}
```

## 模式扩展机制

- `Mode` 使用策略模式与 SPI 注册机制，`ModeRegistry` 管理模式实例。
- 新增模式只需实现 `Mode`，并通过 Spring 条件装配或 `META-INF/services` 注册。
- `Agent` 注入 `Mode` 时支持名称选择与动态切换。

```java
public interface ModeRegistry {
  Mode get(String name);
  void register(Mode mode);
  Collection<Mode> all();
}
```

## 工具系统

- `Tool` 统一接口，定义名称、Schema、调用入口。
- `ToolRegistry` 管理工具集合，支持标签分类、权限控制、速率限制、缓存策略。
- 提供常用工具适配：Web 搜索、HTTP 请求、SQL 查询、文件读写、向量检索。
- 支持参数校验与 JSON Schema 生成，便于函数调用式模型对接。

```java
public interface ToolRegistry {
  Tool get(String name);
  void register(Tool tool);
  List<Tool> list();
}
```

## 注解驱动工具注册

- 使用注解 `@Tool` 与 Spring 组件扫描自动注册工具，降低集成成本。
- 工具通过 `ToolExecutor` 暴露执行入口，并提供 JSON Schema 参数描述，便于函数调用模型对接。

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Component
public @interface Tool {
  String name();
  String description();
}

public interface ToolExecutor {
  Object execute(Map<String, Object> args) throws Exception;
  Map<String, Object> getParametersSchema();
}
```

## 记忆系统

- 短期记忆：对话历史与步骤轨迹，保存在 `AgentMemory` 的会话级存储中。
- 长期记忆：向量化嵌入，存入 `VectorStore`，用于检索增强提示。
- 检索增强：进入模式执行前调用 `VectorStore.similaritySearch` 合并上下文。

## LLM 接入

- `LlmClient` 抽象适配 OpenAI、Azure OpenAI、DeepSeek、Qwen 等厂商。
- 支持同步与流式输出、函数调用、系统指令。
- 通过 Spring 配置管理模型、超时、重试、并发限制、令牌预算。

## 提示工程

- `PromptTemplate` 支持变量渲染、分区模板、模式专属模板集。
- ReAct 模板包含角色设定、工具说明、思维轨迹占位符、停止条件。

## 执行管线

- `AgentRunner` 负责生命周期管理，包含初始化、上下文增强、模式执行、结果回传。
- `Trace` 贯穿每一步，事件总线同步推送步骤、耗时、错误、输出。
- 支持同步调用与异步执行，异步通过任务队列或 Reactor。

## 并发与容错

- 超时、重试、降级、熔断，可选集成 Resilience4j。
- 工具调用隔离线程池，防止阻塞 LLM 请求。
- 暂存与断点恢复，避免长链路中断导致丢失进度。

## 可观测性

- 统一日志结构化输出，标注 `agentId`、`traceId`、`step`、`mode`。
- 指标接入 Micrometer，统计调用次数、耗时、错误率、令牌使用量。
- 链路追踪可选接入 OpenTelemetry。

## 安全

- 凭据通过 Spring `Environment` 与密钥管理方案载入，不落盘明文。
- 工具权限与沙箱策略，限制文件与网络访问范围。
- 提示注入防护与输出校验，避免越权调用。

## Spring Boot 集成

- 自动装配：
    - `MultiAgentAutoConfiguration` 提供 `AgentManager`、`ModeRegistry`、`ToolRegistry`、`LlmClient` 等 Bean。
    - 条件装配根据 `multiagent.enabled`、`multiagent.llm.provider`、`multiagent.modes` 等属性。
- 配置示例：

```yaml
agent:
  enabled: true
  llm:
    provider: openai
    model: gpt-4o
    apiKey: ${OPENAI_API_KEY}
    baseUrl: https://api.openai.com/v1
    timeoutMs: 60000
  react:
    maxSteps: 5
    stream: true
  memory:
    backend: in-memory
    session: stateless
```

- 使用示例：

```java
@SpringBootApplication
public class DemoApp {
  public static void main(String[] args) {
    SpringApplication.run(DemoApp.class, args);
  }
}

@RestController
public class AgentController {
  private final AgentManager agentManager;

  public AgentController(AgentManager agentManager) {
    this.agentManager = agentManager;
  }

  @PostMapping("/ask")
  public String ask(@RequestBody String question) {
    Agent agent = agentManager.defaultAgent("react");
    ModeResult result = agentManager.run(agent, Message.user(question));
    return result.finalAnswer();
  }
}
```

## REST API 规范

- 路径：`POST /api/agent/query`
- 请求体：`{"question": "..."}`；响应体：`{"answer": "..."}`。
- 控制器示例：

```java
@RestController
@RequestMapping("/api/agent")
public class AgentController {
  private final ReActAgent agent;
  public AgentController(ReActAgent agent) { this.agent = agent; }
  @PostMapping("/query")
  public ResponseEntity<Map<String, String>> query(@RequestBody Map<String, String> req) {
    String answer = agent.run(req.get("question"));
    return ResponseEntity.ok(Map.of("answer", answer));
  }
}
```

## 模块结构建议

- `multiagent-core`
    - 接口：`Agent`、`Mode`、`Tool`、`AgentMemory`、`VectorStore`、`LlmClient`、`EventBus`
    - 执行：`AgentRunner`、`ModeRegistry`、`ToolRegistry`、`PromptTemplate`
    - 模型：`Message`、`Thought`、`Action`、`Observation`、`Trace`
- `multiagent-modes-react`
    - `ReactMode`、`ReactParser`、`ReactPrompt`
- `multiagent-adapters-llm-*`
    - OpenAI、Azure、Qwen、DeepSeek 等实现
- `multiagent-adapters-vector-*`
    - InMemory、PgVector、Milvus、Elastic、Qdrant
- `multiagent-tools-*`
    - Http、WebSearch、SQL、Filesystem、KnowledgeBase
- `multiagent-spring-boot-starter`
    - AutoConfiguration、Properties、条件装配、默认 Bean
- `multiagent-examples`
    - Spring Boot 演示、端到端示例

## 项目结构（Maven）

- `src/main/java/com/example/reactagent/`
    - `ReactAgentApplication.java`
    - `agent/ReActAgent.java`、`agent/ParsedAction.java`
    - `llm/LLMProvider.java`、`llm/OpenAILLMProvider.java`、`llm/LLMProperties.java`
    - `tool/ToolExecutor.java`、`tool/ToolRegistry.java`、`tool/annotation/Tool.java`
    - `memory/Memory.java`、`memory/SimpleMemory.java`
    - `parser/ReActOutputParser.java`
    - `web/AgentController.java`
- `src/main/resources/`
    - `application.yml`
    - `templates/react-prompt.txt`

## 优化摘要（源自 qwen.md）

- 注解驱动工具注册与自动扫描，降低扩展门槛。
- LLM 提供者通过条件装配按 `provider` 切换，便于运维。
- REST API 统一入口，适配高并发的无状态 Memory 设计。
- Prompt 模板化与历史轨迹拼接，支持 Markdown/JSON 输出解析。
- 未来扩展：SSE 流式响应、RAG 向量检索、会话记忆与权限控制。

## 数据存储与缓存

- 会话状态可选 InMemory、Redis。
- 向量检索可选内存、PgVector、Milvus、Qdrant，依据规模选择。
- 缓存层用于工具结果与相似度检索，支持 TTL 与命中统计。

## 流式输出

- LLM 流式响应通过 `SSE` 或 `WebSocket` 推送，事件总线发布 `LlmResponseChunk`。
- ReAct 步骤流式展示，便于 UI 实时可视化。

## 错误处理策略

- 分类错误：提示错误、工具错误、外部依赖错误、超时与预算错误。
- 每类错误有独立重试与回退策略，必要时切换模式或终止。

## 性能与成本控制

- 令牌预算管理，控制长上下文与工具输出长度。
- 模型选择与温度、TopP、最大输出长度通过配置统一管理。
- 合理的 `maxSteps` 与提前停机策略减少无效循环。

## 测试与质量保证

- 单元测试：接口契约与解析逻辑，使用 JUnit 5。
- 集成测试：SpringBootTest 校验自动装配与端到端流程。
- 端到端回归用例：覆盖 ReAct 常见路径与异常路径。
- 代码风格与静态检查：Spotless、Checkstyle、ErrorProne。

## 原型实现建议

- 首先完成 `multiagent-core` 的接口与 `AgentRunner`。
- 实现 `multiagent-modes-react` 的最小 ReAct 循环与解析器。
- 提供 `multiagent-adapters-llm-openai` 与 `vector-inmemory` 的基础适配。
- 完成 `multiagent-spring-boot-starter` 自动装配与配置示例，发布 Demo。

## 路线图

- 支持更多模式：Plan-and-Execute、Tree-of-Thought、Graph-of-Thought。
- 加入工具学习与工具选择器，通过模型辅助选择最优工具。
- 增加记忆整合策略：权重、摘要、主题聚类。
- 引入工作流编排与可视化，支持可拖拽的步骤编辑。
- 提供多代理协作样例，支持角色分工与消息路由。

## 交付物

- 技术方案本文档。
- 最小可运行的 Spring Boot Demo。
- 可插拔的模式、工具、LLM、向量库适配层。