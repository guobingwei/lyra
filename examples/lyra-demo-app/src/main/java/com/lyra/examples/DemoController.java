package com.lyra.examples;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lyra.agent.agent.Agent;
import com.lyra.agent.agent.AgentManager;
import com.lyra.agent.agent.Message;
import com.lyra.agent.agent.ModeResult;
import com.lyra.agent.autoconfigure.LyraAgentProperties;
import com.lyra.agent.event.AgentEvent;
import com.lyra.agent.event.Event;
import com.lyra.agent.event.EventListener;
import com.lyra.agent.event.SimpleEventBus;
import com.lyra.agent.llm.LLMProvider;
import com.lyra.agent.tool.ToolRegistry;
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
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public DemoController(AgentManager agentManager, LLMProvider llmProvider, 
                          ToolRegistry toolRegistry, 
                          LyraAgentProperties properties, 
                          ResourceLoader resourceLoader) {
        this.agentManager = agentManager;
        this.llmProvider = llmProvider;
        this.toolRegistry = toolRegistry;
        this.properties = properties;
        this.resourceLoader = resourceLoader;
    }

    @GetMapping(value = "/demo/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@RequestParam("query") String query) {
        SseEmitter emitter = new SseEmitter(180000L); // 3 minutes timeout

        executor.submit(() -> {
            try {
                // 1. 创建独立的 EventBus
                SimpleEventBus eventBus = new SimpleEventBus();
                
                // 2. 订阅事件并推送到 SSE
                EventListener listener = event -> {
                    try {
                        Map<String, Object> data = new HashMap<>();
                        data.put("type", event.type());
                        data.put("data", event.payload());
                        emitter.send(SseEmitter.event().data(objectMapper.writeValueAsString(data)));
                    } catch (IOException e) {
                        emitter.completeWithError(e);
                    }
                };
                
                // 订阅所有感兴趣的事件
                eventBus.subscribe("agent.start", listener);
                eventBus.subscribe("agent.step", listener);
                eventBus.subscribe("agent.thought", listener);
                eventBus.subscribe("agent.tool.start", listener);
                eventBus.subscribe("agent.tool.end", listener);
                eventBus.subscribe("agent.finish", listener);
                eventBus.subscribe("agent.error", listener);

                // 5. Get the default ReAct agent
                Agent agent = agentManager.defaultAgent("react");

                // 6. Execute Agent with the new architecture
                Message inputMessage = Message.user(query);
                ModeResult result = agentManager.run(agent, inputMessage);
                
                // 5. 完成 SSE
                emitter.send(SseEmitter.event().name("DONE").data(""));
                emitter.complete();

            } catch (Exception e) {
                try {
                    emitter.send(SseEmitter.event().name("ERROR").data(e.getMessage()));
                } catch (IOException ex) {
                    // ignore
                }
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }
}
