package com.lyra.agent.web;

import com.lyra.agent.agent.AgentManager;
import com.lyra.agent.agent.Message;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@ConditionalOnProperty(prefix = "agent.api", name = "expose", havingValue = "true")
@RequestMapping("/api/agent")
/**
 * 提供与智能体交互的 REST 接口。
 */
public class AgentController {
    private final AgentManager agentManager;

    public AgentController(AgentManager agentManager) {
        this.agentManager = agentManager;
    }

    /**
     * 接收用户问题并返回智能体答案。
     *
     * @param req 请求体，包含字段 {@code question}
     * @return 包含 {@code answer} 的响应
     */
    @PostMapping("/query")
    public ResponseEntity<Map<String, String>> query(@RequestBody Map<String, String> req) {
        // Get the default ReAct agent
        com.lyra.agent.agent.Agent agent = agentManager.defaultAgent("react");
        // Create a user message
        Message message = Message.user(req.get("question"));
        // Run the agent
        com.lyra.agent.agent.ModeResult result = agentManager.run(agent, message);
        String answer = result.getFinalAnswer();
        java.util.Map<String, String> resp = new java.util.HashMap<>();
        resp.put("answer", answer);
        return ResponseEntity.ok(resp);
    }
}