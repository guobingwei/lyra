package com.lyra.agent.llm;

import com.lyra.agent.autoconfigure.LyraAgentProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

public class DeepSeekLLMProvider implements LLMProvider {
    private static final Logger logger = LoggerFactory.getLogger(DeepSeekLLMProvider.class);
    private final LyraAgentProperties.Llm config;
    private final RestTemplate restTemplate;

    public DeepSeekLLMProvider(LyraAgentProperties.Llm config) {
        this.config = config;
        logger.info("Initializing DeepSeekLLMProvider with model: {}, baseUrl: {}", 
            config.getModel(), config.getBaseUrl());
        logger.info("API Key configured: {}", config.getApiKey() != null && !config.getApiKey().isEmpty());
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(config.getTimeoutMs());
        factory.setReadTimeout(config.getTimeoutMs());
        this.restTemplate = new RestTemplate(factory);
    }

    @Override
    public String generate(String prompt) {
        logger.info("========== DeepSeek API Call ==========");
        logger.info("DeepSeek generate called with prompt length: {}", prompt.length());
        logger.info("Full prompt being sent to DeepSeek:\n{}", prompt);
        logger.info("========================================\n");
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (config.getApiKey() != null && !config.getApiKey().isEmpty()) {
            headers.setBearerAuth(config.getApiKey());
            logger.debug("API Key set in Authorization header (Bearer token)");
        } else {
            logger.warn("No API Key configured for DeepSeek");
        }

        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", prompt);

        String model = config.getModel() == null || config.getModel().isEmpty() ? "deepseek-chat" : config.getModel();
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", Collections.singletonList(message));
        requestBody.put("temperature", 0.0);
        
        logger.info("Request body prepared with model: {}", model);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            String base = (config.getBaseUrl() == null || config.getBaseUrl().isEmpty())
                    ? "https://api.deepseek.com/v1"
                    : config.getBaseUrl();
            String url = base + "/chat/completions";
            logger.info("Calling DeepSeek API at: {}", url);
            
            ResponseEntity<Map<String, Object>> response = restTemplate.postForEntity(url, entity, 
                (Class<Map<String, Object>>) (Class<?>) Map.class);
            logger.info("DeepSeek API response status: {}", response.getStatusCode());
            
            if (response.getBody() == null) {
                logger.error("Empty response body from DeepSeek API");
                return "Error: Empty response body";
            }
            
            logger.debug("Response body received: {}", response.getBody());
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");
            if (choices == null || choices.isEmpty()) {
                logger.error("No choices in DeepSeek API response");
                return "Error: No choices in response";
            }
            Map<String, Object> firstChoice = choices.get(0);
            @SuppressWarnings("unchecked")
            Map<String, Object> msg = (Map<String, Object>) firstChoice.get("message");
            String content = msg == null ? "" : (String) msg.get("content");
            logger.info("========== DeepSeek Response ==========");
            logger.info("DeepSeek response content length: {}", content.length());
            logger.info("Full response content:\n{}", content);
            logger.info("========================================\n");
            return content;
        } catch (Exception e) {
            logger.error("Error calling DeepSeek API", e);
            logger.error("Error details: {}", e.getMessage());
            if (e.getCause() != null) {
                logger.error("Root cause: {}", e.getCause().getMessage());
            }
            return "Error calling LLM: " + e.getMessage();
        }
    }
}

