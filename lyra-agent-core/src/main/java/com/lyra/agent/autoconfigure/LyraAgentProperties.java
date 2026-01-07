package com.lyra.agent.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("agent")
/**
 * Lyra Agent 配置属性。
 * 包含基础开关、最大步数、Prompt 路径与 LLM/API 子配置。
 */
public class LyraAgentProperties {
    private boolean enabled = true;
    private int maxSteps = 5;
    private String promptPath = "classpath:templates/react-prompt.txt";
    private final Llm llm = new Llm();
    private final Vector vector = new Vector();
    private final Api api = new Api();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getMaxSteps() {
        return maxSteps;
    }

    public void setMaxSteps(int maxSteps) {
        this.maxSteps = maxSteps;
    }

    public String getPromptPath() {
        return promptPath;
    }

    public void setPromptPath(String promptPath) {
        this.promptPath = promptPath;
    }

    public Llm getLlm() {
        return llm;
    }

    public Vector getVector() {
        return vector;
    }

    public Api getApi() {
        return api;
    }

    /**
     * LLM 相关配置。
     */
    public static class Llm {
        private String provider = "openai";
        private String model = "gpt-4o";
        private String embeddingModel = "text-embedding-3-small";
        private String apiKey;
        private String baseUrl = "https://api.openai.com/v1";
        private int timeoutMs = 60000;

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public String getEmbeddingModel() {
            return embeddingModel;
        }

        public void setEmbeddingModel(String embeddingModel) {
            this.embeddingModel = embeddingModel;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public int getTimeoutMs() {
            return timeoutMs;
        }

        public void setTimeoutMs(int timeoutMs) {
            this.timeoutMs = timeoutMs;
        }
    }

    /**
     * 向量存储相关配置。
     */
    public static class Vector {
        private String store = "in-memory"; // in-memory, milvus
        private Milvus milvus = new Milvus();

        public String getStore() {
            return store;
        }

        public void setStore(String store) {
            this.store = store;
        }

        public Milvus getMilvus() {
            return milvus;
        }

        public void setMilvus(Milvus milvus) {
            this.milvus = milvus;
        }

        public static class Milvus {
            private String host = "localhost";
            private int port = 19530;
            private String collectionName = "lyra_knowledge";
            private int dimension = 1536;
            private String username;
            private String password;

            public String getHost() { return host; }
            public void setHost(String host) { this.host = host; }
            public int getPort() { return port; }
            public void setPort(int port) { this.port = port; }
            public String getCollectionName() { return collectionName; }
            public void setCollectionName(String collectionName) { this.collectionName = collectionName; }
            public int getDimension() { return dimension; }
            public void setDimension(int dimension) { this.dimension = dimension; }
            public String getUsername() { return username; }
            public void setUsername(String username) { this.username = username; }
            public String getPassword() { return password; }
            public void setPassword(String password) { this.password = password; }
        }
    }

    /**
     * API 暴露相关配置。
     */
    public static class Api {
        private boolean expose = false;

        public boolean isExpose() {
            return expose;
        }

        public void setExpose(boolean expose) {
            this.expose = expose;
        }
    }
}