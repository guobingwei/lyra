package com.lyra.examples;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lyra.agent.agent.Agent;
import com.lyra.agent.agent.AgentManager;
import com.lyra.agent.agent.Message;
import com.lyra.agent.agent.ModeResult;
import com.lyra.agent.agent.ReactMode;
import com.lyra.agent.agent.SimpleAgentManager;
import com.lyra.agent.agent.SimpleModeRegistry;
import com.lyra.agent.autoconfigure.LyraAgentProperties;
import com.lyra.agent.event.AgentEvent;
import com.lyra.agent.event.Event;
import com.lyra.agent.event.EventBus;
import com.lyra.agent.event.EventListener;
import com.lyra.agent.event.SimpleEventBus;
import com.lyra.agent.llm.LLMProvider;
import com.lyra.agent.tool.ToolRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
public class DemoController {

    private final AgentManager agentManager;
    private final LLMProvider llmProvider;
    private final ToolRegistry toolRegistry;
    private final LyraAgentProperties properties;
    private final ResourceLoader resourceLoader;
    private final EventBus eventBus;
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final Logger logger = LoggerFactory.getLogger(DemoController.class);

    public DemoController(AgentManager agentManager, LLMProvider llmProvider, 
                          ToolRegistry toolRegistry, 
                          LyraAgentProperties properties, 
                          ResourceLoader resourceLoader,
                          EventBus eventBus) {
        this.agentManager = agentManager;
        this.llmProvider = llmProvider;
        this.toolRegistry = toolRegistry;
        this.properties = properties;
        this.resourceLoader = resourceLoader;
        this.eventBus = eventBus;
    }

    @GetMapping(value = "/demo/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@RequestParam("query") String query) {
        logger.info("Received query: {}", query);
        SseEmitter emitter = new SseEmitter(180000L); // 3 minutes timeout

        executor.submit(() -> {
            // Create a local EventBus for this request to avoid cross-request contamination
            SimpleEventBus localEventBus = new SimpleEventBus();
            
            try {
                logger.info("Starting agent execution for query: {}", query);
                
                // 1. Subscribe to events and push to SSE
                EventListener listener = event -> {
                    try {
                        logger.debug("Received event: {} with data: {}", event.type(), event.payload());
                        Map<String, Object> data = new HashMap<>();
                        data.put("type", event.type());
                        data.put("data", event.payload());
                        emitter.send(SseEmitter.event().data(objectMapper.writeValueAsString(data)));
                    } catch (IOException e) {
                        logger.error("Error sending event", e);
                        emitter.completeWithError(e);
                    }
                };
                
                // Subscribe to all events of interest on the local event bus
                localEventBus.subscribe("agent.start", listener);
                localEventBus.subscribe("agent.step", listener);
                localEventBus.subscribe("agent.thought", listener);
                localEventBus.subscribe("agent.stream.chunk", listener);
                localEventBus.subscribe("agent.tool.start", listener);
                localEventBus.subscribe("agent.tool.end", listener);
                localEventBus.subscribe("agent.finish", listener);
                localEventBus.subscribe("agent.error", listener);
                localEventBus.subscribe("agent.timeout", listener);

                // 2. Get the default ReAct agent with the local event bus
                logger.info("Getting default ReAct agent");
                // Create a temporary AgentManager with the local event bus for this request
                SimpleModeRegistry modeRegistry = new SimpleModeRegistry();
                // Register ReactMode
                Resource promptTemplate = resourceLoader.getResource("classpath:templates/react-prompt.txt");
                ReactMode reactMode = new ReactMode(llmProvider, toolRegistry, localEventBus, properties.getMaxSteps(), promptTemplate);
                modeRegistry.register(reactMode);
                
                AgentManager localAgentManager = new SimpleAgentManager(
                    modeRegistry,
                    llmProvider,
                    toolRegistry,
                    localEventBus
                );
                Agent agent = localAgentManager.defaultAgent("react");
                logger.info("Agent retrieved: {}", agent.id());

                // 3. Execute Agent with the new architecture
                Message inputMessage = Message.user(query);
                logger.info("Executing agent with input message: {}", query);
                ModeResult result = localAgentManager.run(agent, inputMessage);
                logger.info("Agent execution completed, result is final: {}", result.isFinal());
                
                // 4. Complete SSE
                logger.info("Sending DONE event");
                emitter.send(SseEmitter.event().name("DONE").data(""));
                emitter.complete();
                logger.info("SSE connection completed");

            } catch (Exception e) {
                logger.error("Error during agent execution", e);
                try {
                    emitter.send(SseEmitter.event().name("ERROR").data(e.getMessage()));
                } catch (IOException ex) {
                    logger.error("Error sending error event", ex);
                    // ignore
                }
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }
}
