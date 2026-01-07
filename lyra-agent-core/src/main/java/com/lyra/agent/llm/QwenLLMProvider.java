package com.lyra.agent.llm;

import com.lyra.agent.autoconfigure.LyraAgentProperties;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QwenLLMProvider implements LLMProvider {

    private final LyraAgentProperties.Llm config;
    private final RestTemplate restTemplate;

    public QwenLLMProvider(LyraAgentProperties.Llm config) {
        this.config = config;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(config.getTimeoutMs());
        factory.setReadTimeout(config.getTimeoutMs());
        this.restTemplate = new RestTemplate(factory);
    }

    @Override
    public String generate(String prompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (config.getApiKey() != null && !config.getApiKey().isEmpty()) {
            headers.setBearerAuth(config.getApiKey());
        }

        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", prompt);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", config.getModel() == null || config.getModel().isEmpty() ? "qwen-plus" : config.getModel());
        requestBody.put("messages", Collections.singletonList(message));
        requestBody.put("temperature", 0.0);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            String base = (config.getBaseUrl() == null || config.getBaseUrl().isEmpty())
                    ? "https://dashscope.aliyuncs.com/compatible/v1"
                    : config.getBaseUrl();
            String url = base + "/chat/completions";
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            if (response.getBody() == null) {
                return "Error: Empty response body";
            }
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");
            if (choices == null || choices.isEmpty()) {
                return "Error: No choices in response";
            }
            Map<String, Object> firstChoice = choices.get(0);
            Map<String, Object> msg = (Map<String, Object>) firstChoice.get("message");
            return msg == null ? "" : (String) msg.get("content");
        } catch (Exception e) {
            return "Error calling LLM: " + e.getMessage();
        }
    }
}

