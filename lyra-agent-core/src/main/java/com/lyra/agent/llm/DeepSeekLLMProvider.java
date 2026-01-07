package com.lyra.agent.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lyra.agent.autoconfigure.LyraAgentProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

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

    @Override
    public void generateStream(String prompt, Consumer<StreamChunk> chunkConsumer) {
        logger.info("========== DeepSeek Streaming API Call ==========");
        logger.info("DeepSeek generateStream called with prompt length: {}", prompt.length());
        logger.info("Full prompt being sent to DeepSeek:\n{}", prompt);
        logger.info("========================================\n");

        try {
            String base = (config.getBaseUrl() == null || config.getBaseUrl().isEmpty())
                    ? "https://api.deepseek.com/v1"
                    : config.getBaseUrl();
            String urlString = base + "/chat/completions";
            
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "text/event-stream");
            
            if (config.getApiKey() != null && !config.getApiKey().isEmpty()) {
                connection.setRequestProperty("Authorization", "Bearer " + config.getApiKey());
            }
            
            connection.setDoOutput(true);
            connection.setConnectTimeout(config.getTimeoutMs());
            connection.setReadTimeout(config.getTimeoutMs());

            // Build request body
            Map<String, Object> message = new HashMap<>();
            message.put("role", "user");
            message.put("content", prompt);

            String model = config.getModel() == null || config.getModel().isEmpty() ? "deepseek-chat" : config.getModel();
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", Collections.singletonList(message));
            requestBody.put("temperature", 0.0);
            requestBody.put("stream", true);

            ObjectMapper mapper = new ObjectMapper();
            String jsonBody = mapper.writeValueAsString(requestBody);
            
            logger.info("Sending streaming request to: {}", urlString);
            
            // Write request body
            connection.getOutputStream().write(jsonBody.getBytes("UTF-8"));
            connection.getOutputStream().flush();

            // Read streaming response
            int responseCode = connection.getResponseCode();
            logger.info("DeepSeek streaming response code: {}", responseCode);
            
            if (responseCode == 200) {
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), "UTF-8"));
                
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("data: ")) {
                        String data = line.substring(6);
                        
                        if ("[DONE]".equals(data)) {
                            logger.info("Stream completed: [DONE]");
                            chunkConsumer.accept(new StreamChunk("", true, Map.of(), "stop"));
                            break;
                        }
                        
                        try {
                            JsonNode json = mapper.readTree(data);
                            JsonNode choices = json.get("choices");
                            
                            if (choices != null && choices.isArray() && choices.size() > 0) {
                                JsonNode firstChoice = choices.get(0);
                                JsonNode delta = firstChoice.get("delta");
                                JsonNode contentNode = delta != null ? delta.get("content") : null;
                                
                                String content = contentNode != null ? contentNode.asText() : "";
                                String finishReason = firstChoice.has("finish_reason") && 
                                                     !firstChoice.get("finish_reason").isNull() 
                                                     ? firstChoice.get("finish_reason").asText() : null;
                                
                                boolean isDone = finishReason != null;
                                
                                if (!content.isEmpty() || isDone) {
                                    logger.debug("Stream chunk: content='{}', done={}", content, isDone);
                                    chunkConsumer.accept(new StreamChunk(
                                        content, 
                                        isDone, 
                                        Map.of(), 
                                        finishReason != null ? finishReason : "")
                                    );
                                }
                            }
                        } catch (Exception e) {
                            logger.error("Error parsing stream chunk: {}", data, e);
                        }
                    }
                }
                
                reader.close();
            } else {
                BufferedReader errorReader = new BufferedReader(
                    new InputStreamReader(connection.getErrorStream(), "UTF-8"));
                StringBuilder errorMessage = new StringBuilder();
                String errorLine;
                while ((errorLine = errorReader.readLine()) != null) {
                    errorMessage.append(errorLine);
                }
                errorReader.close();
                
                String error = "DeepSeek API error (" + responseCode + "): " + errorMessage.toString();
                logger.error(error);
                chunkConsumer.accept(new StreamChunk("Error: " + error, true, Map.of(), "error"));
            }
            
            connection.disconnect();
            logger.info("DeepSeek streaming completed");
            
        } catch (Exception e) {
            logger.error("Error calling DeepSeek streaming API", e);
            chunkConsumer.accept(new StreamChunk(
                "Error calling LLM: " + e.getMessage(), 
                true, 
                Map.of(), 
                "error")
            );
        }
    }
}

