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

/**
 * OpenAI 嵌入模型实现。
 * 调用 /embeddings 接口。
 */
public class OpenAIEmbeddingModel implements EmbeddingModel {
    private final LyraAgentProperties.Llm config;
    private final RestTemplate restTemplate;

    public OpenAIEmbeddingModel(LyraAgentProperties.Llm config) {
        this.config = config;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(config.getTimeoutMs());
        factory.setReadTimeout(config.getTimeoutMs());
        this.restTemplate = new RestTemplate(factory);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Double> embed(String text) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (config.getApiKey() != null && !config.getApiKey().isEmpty()) {
            headers.setBearerAuth(config.getApiKey());
        }

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("input", text);
        requestBody.put("model", config.getEmbeddingModel());

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            String url = config.getBaseUrl() + "/embeddings";
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

            if (response.getBody() == null) {
                throw new RuntimeException("Empty response from embedding API");
            }

            List<Map<String, Object>> data = (List<Map<String, Object>>) response.getBody().get("data");
            if (data == null || data.isEmpty()) {
                throw new RuntimeException("No embedding data in response");
            }

            return (List<Double>) data.get(0).get("embedding");

        } catch (Exception e) {
            throw new RuntimeException("Error calling embedding API: " + e.getMessage(), e);
        }
    }
}
