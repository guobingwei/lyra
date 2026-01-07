# Lyra Agent Framework

A general-purpose agent framework built with Spring Boot, designed as a reusable starter for building intelligent agent applications.

## Project Structure

This project follows a multi-module Maven structure:

```
lyra/
├── pom.xml                          # Parent POM (aggregator)
├── lyra-agent-core/                 # Core framework module
│   ├── pom.xml                      # Core module POM
│   └── src/
│       └── main/java/com/lyra/agent/
│           ├── agent/               # Agent core logic
│           ├── llm/                 # LLM providers
│           ├── memory/              # Memory management
│           ├── tool/                # Tool system
│           └── ...
└── examples/
    └── lyra-demo-app/               # Demo application
        ├── pom.xml                  # Demo app POM
        └── src/main/
            ├── java/com/lyra/examples/
            │   ├── LyraDemoApplication.java
            │   └── DemoController.java
            └── resources/
                └── static/
                    └── index.html   # Web UI
```

## Modules

### 1. lyra-agent-core (Spring Boot Starter)

The core framework that provides:
- **Agent Framework**: ReAct agent implementation with multi-step reasoning
- **LLM Integration**: Support for multiple LLM providers (DeepSeek, OpenAI, Gemini, Qwen)
- **Streaming Support**: Real-time streaming output from LLMs
- **Tool System**: Extensible tool registry and execution framework
- **Memory Management**: Vector store integration (In-memory, Milvus)
- **Event System**: Pub/sub event bus for agent lifecycle events

**Artifact**: `com.lyra:lyra-agent-spring-boot-starter:0.1.0-SNAPSHOT`

### 2. examples/lyra-demo-app (Demo Application)

A demonstration application that shows how to use the framework:
- Web-based UI for interacting with agents
- Real-time streaming output display
- Structured logging panel
- Example controller showing framework integration

## Getting Started

### Building the Project

From the root directory:

```bash
mvn clean install
```

This will:
1. Build the core framework (`lyra-agent-core`)
2. Install it to local Maven repository
3. Build the demo application (`lyra-demo-app`)

### Running the Demo Application

```bash
cd lyra-demo-app
mvn spring-boot:run
```

Then open http://localhost:8080 in your browser.

### Using the Framework in Your Project

Add the dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>com.lyra</groupId>
    <artifactId>lyra-agent-spring-boot-starter</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

Configure in `application.yml`:

```yaml
agent:
  enabled: true
  llm:
    provider: deepseek  # or openai, gemini, qwen
    model: deepseek-chat
    api-key: your-api-key-here
    base-url: https://api.deepseek.com/v1
    timeoutMs: 60000
  max-steps: 5
```

## Features

- ✅ **Multi-LLM Support**: DeepSeek, OpenAI, Gemini, Qwen
- ✅ **Streaming Output**: Real-time token-by-token streaming
- ✅ **ReAct Agent**: Reasoning + Acting pattern implementation
- ✅ **Tool System**: Extensible tool framework
- ✅ **Memory Management**: Vector store integration
- ✅ **Event-Driven**: Pub/sub architecture for agent events
- ✅ **Spring Boot Auto-Configuration**: Easy integration

## Architecture

The framework follows a clean, modular design:

1. **Agent Layer**: High-level agent orchestration
2. **LLM Layer**: Provider abstraction with streaming support
3. **Tool Layer**: Tool registry and execution
4. **Memory Layer**: Vector store and memory management
5. **Event Layer**: Event bus for cross-component communication

## Development

### Adding a New LLM Provider

1. Implement `LLMProvider` interface
2. Override `generate()` and `generateStream()` methods
3. Register in Spring configuration

### Adding a New Tool

1. Create a class with `@Tool` annotation
2. Implement `ToolExecutor` interface
3. Spring will auto-register it

## License

[Your License Here]
