package com.lyra.agent.llm;

import com.lyra.agent.autoconfigure.LyraAgentProperties;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Google Gemini 提供者实现。
 * 通过 REST API 调用 Google Gemini 接口。
 */
public class GeminiLLMProvider implements LLMProvider {
    private final LyraAgentProperties.Llm config;
    private final RestTemplate restTemplate;

    /**
     * 使用配置初始化。
     *
     * @param config LLM 配置
     */
    public GeminiLLMProvider(LyraAgentProperties.Llm config) {
        this.config = config;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(config.getTimeoutMs());
        factory.setReadTimeout(config.getTimeoutMs());
        this.restTemplate = new RestTemplate(factory);
    }

    /**
     * 生成响应。
     * 调用 Gemini generateContent 接口。
     *
     * @param prompt 输入提示
     * @return 文本响应
     */
    @Override
    public String generate(String prompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (config.getApiKey() != null && !config.getApiKey().isEmpty()) {
            headers.add("x-goog-api-key", config.getApiKey());
        }

        // Gemini API payload structure
        // { "contents": [{ "parts": [{ "text": "..." }] }] }
        Map<String, Object> part = new HashMap<>();
        part.put("text", prompt);
        
        Map<String, Object> content = new HashMap<>();
        content.put("parts", Collections.singletonList(part));
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("contents", Collections.singletonList(content));

        // Gemini uses API key in query param usually, but let's check config.
        // Base URL usually: https://generativelanguage.googleapis.com/v1beta
        // Full URL: BASE_URL + /models/MODEL_NAME:generateContent?key=API_KEY
        
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            String baseUrl = "https://generativelanguage.googleapis.com";
            // Force v1beta path to ensure compatibility
            String apiVersion = "/v1beta";
            String configuredBase = config.getBaseUrl();
            if (configuredBase != null && configuredBase.contains("generativelanguage.googleapis.com")) {
                // Respect custom domain if provided (without trailing version)
                baseUrl = configuredBase.replaceAll("/v1(beta)?$", "");
                apiVersion = configuredBase.endsWith("/v1") ? "/v1" : "/v1beta";
            }

            // Handle model name
            String model = config.getModel();
            if (model == null || model.isEmpty()) {
                model = "gemini-pro";
            }

            String url = String.format("%s%s/models/%s:generateContent", baseUrl, apiVersion, model);

            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

            if (response.getBody() == null) {
                return "Error: Empty response body";
            }

            // Parse response
            // { "candidates": [{ "content": { "parts": [{ "text": "..." }] } }] }
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.getBody().get("candidates");
            if (candidates == null || candidates.isEmpty()) {
                // Safety ratings block?
                return "Error: No candidates in response (blocked?)";
            }
            
            Map<String, Object> firstCandidate = candidates.get(0);
            Map<String, Object> respContent = (Map<String, Object>) firstCandidate.get("content");
            List<Map<String, Object>> parts = (List<Map<String, Object>>) respContent.get("parts");
            
            if (parts == null || parts.isEmpty()) {
                return "";
            }
            
            return (String) parts.get(0).get("text");

        } catch (Exception e) {
            return "Error calling Gemini API: " + e.getMessage();
        }
    }
}
