# Lyra Agent Spring Boot Starter

Lyra 是一个基于 Spring Boot 的智能体 Starter，支持 ReAct 推理模式，提供注解驱动的工具注册、LLM 抽象与自动装配，便于业务项目零侵入接入智能体能力。

## 特性

- 原生支持 ReAct：Thought → Action → Observation → Final Answer
- 注解工具注册：使用 `@Tool` 自动扫描并注册工具
- LLM 抽象：可扩展 OpenAI、Qwen 等提供商
- Spring 自动装配：按 `agent.*` 配置启用与控制
- 可选 REST 接口：按需暴露统一调用入口

## 快速开始

- 安装到本地仓库
    - 在项目根目录执行：`mvn -q -DskipTests install`
- 在业务项目引入依赖
    - `pom.xml` 添加：
      ```xml
      <dependency>
        <groupId>com.lyra</groupId>
        <artifactId>lyra-agent-spring-boot-starter</artifactId>
        <version>0.1.0-SNAPSHOT</version>
      </dependency>
      ```
- 配置 `application.yml`
  ```yaml
  agent:
    enabled: true
    llm:
      provider: openai
      model: gpt-4o
      api-key: ${OPENAI_API_KEY}
      base-url: https://api.openai.com/v1
      timeoutMs: 60000
    max-steps: 5
    api:
      expose: false
  ```
- 注入与调用
  ```java
  @Autowired
  private com.lyra.agent.agent.ReActAgent agent;
  String answer = agent.run("你的问题");
  ```

## 示例项目

- 位置：`examples/lyra-demo-app`
- 启动：在示例目录执行 `mvn -q -DskipTests spring-boot:run`
- 接口：`POST /demo/query` 请求体 `{"question":"..."}`

## 关键文件

- 自动配置：`src/main/java/com/lyra/agent/autoconfigure/LyraAgentAutoConfiguration.java:1`
- 属性绑定：`src/main/java/com/lyra/agent/autoconfigure/LyraAgentProperties.java:4`
- 智能体核心：`src/main/java/com/lyra/agent/agent/ReActAgent.java:1`
- 工具体系：
    - 注解：`src/main/java/com/lyra/agent/tool/annotation/Tool.java:1`
    - 注册：`src/main/java/com/lyra/agent/tool/ToolRegistry.java:1`
    - 示例：`src/main/java/com/lyra/agent/tool/impl/SearchTool.java:7`
- 解析与记忆：
    - 解析器：`src/main/java/com/lyra/agent/parser/ReActOutputParser.java:1`
    - 记忆：`src/main/java/com/lyra/agent/memory/SimpleMemory.java:1`

## 扩展工具

- 在业务项目中实现并标注注解：
  ```java
  @com.lyra.agent.tool.annotation.Tool(name = "search", description = "Web search")
  public class SearchTool implements com.lyra.agent.tool.ToolExecutor {
    public Object execute(java.util.Map<String,Object> args) { return "Result"; }
    public java.util.Map<String,Object> getParametersSchema() {
      java.util.Map<String,Object> p = new java.util.HashMap<>(); p.put("type","string");
      java.util.Map<String,Object> s = new java.util.HashMap<>(); s.put("query", p); return s;
    }
  }
  ```

## 运行要求

- Java 17+
- Spring Boot 3.3+
- Maven 环境可用（或使用 Maven Wrapper）

## 测试与验证

- 安装 Starter：`mvn -q -DskipTests install`
- 运行示例：`examples/lyra-demo-app` 启动后调用 `POST /demo/query`

## 路线图

- 接入真实 LLM HTTP 调用与流式响应
- 会话记忆与向量检索（Redis/PgVector/Milvus）
- 增强工具权限、限流与重试策略