package com.lyra.agent;

import com.lyra.agent.agent.ReActAgent;
import com.lyra.agent.event.Event;
import com.lyra.agent.event.EventBus;
import com.lyra.agent.llm.LLMProvider;
import com.lyra.agent.tool.ToolExecutor;
import com.lyra.agent.tool.ToolRegistry;
import com.lyra.agent.tool.impl.SearchTool;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

public class ReActAgentTest {

    @Test
    public void testReActFlow() {
        // 1. Mock LLMProvider
        LLMProvider llmProvider = Mockito.mock(LLMProvider.class);

        // Simulate LLM responses:
        // Step 1: LLM decides to search
        String step1Response = "Thought: I need to find the capital of France.\n" +
                "Action: search\n" +
                "Input: {\"query\": \"capital of France\"}";

        // Step 2: LLM receives observation and gives final answer
        String step2Response = "Final Answer: The capital of France is Paris.";

        when(llmProvider.generate(anyString()))
                .thenReturn(step1Response)
                .thenReturn(step2Response);

        // 2. Setup ToolRegistry with SearchTool
        SearchTool searchTool = new SearchTool();
        List<ToolExecutor> tools = Collections.singletonList(searchTool);
        ToolRegistry toolRegistry = new ToolRegistry(tools);

        // 3. Setup Mock EventBus
        EventBus eventBus = Mockito.mock(EventBus.class);

        // 4. Load Prompt Template (using the real one from classpath)
        Resource promptTemplate = new DefaultResourceLoader().getResource("classpath:templates/react-prompt.txt");

        // 5. Create Agent
        ReActAgent agent = new ReActAgent(llmProvider, toolRegistry, eventBus, 5, promptTemplate);

        // 6. Run Agent
        String result = agent.run("What is the capital of France?");

        // 7. Verify
        System.out.println("Agent Result: " + result);
        assertEquals("The capital of France is Paris.", result);

        // Verify LLM was called twice
        Mockito.verify(llmProvider, Mockito.times(2)).generate(anyString());

        // Verify events were published
        Mockito.verify(eventBus, Mockito.atLeastOnce()).publish(any(Event.class));
    }
}
