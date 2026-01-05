package com.lyra.agent.memory;

import com.lyra.agent.llm.EmbeddingModel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 基于内存的向量存储实现。
 * 使用余弦相似度进行匹配。
 */
public class InMemoryVectorStore implements VectorStore {
    private final EmbeddingModel embeddingModel;
    private final Map<String, VectorEntry> store = new ConcurrentHashMap<>();

    public InMemoryVectorStore(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    @Override
    public void upsert(String id, String text, Map<String, Object> metadata) {
        List<Double> vector = embeddingModel.embed(text);
        store.put(id, new VectorEntry(id, text, metadata, vector));
    }

    @Override
    public List<VectorSearchResult> similaritySearch(String text, int k) {
        List<Double> queryVector = embeddingModel.embed(text);

        return store.values().stream()
                .map(entry -> {
                    double score = cosineSimilarity(queryVector, entry.vector);
                    return new VectorSearchResult(entry.id, entry.text, entry.metadata, score);
                })
                .sorted(Comparator.comparingDouble(VectorSearchResult::getScore).reversed())
                .limit(k)
                .collect(Collectors.toList());
    }

    private double cosineSimilarity(List<Double> v1, List<Double> v2) {
        if (v1.size() != v2.size()) {
            throw new IllegalArgumentException("Vector lengths do not match");
        }
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < v1.size(); i++) {
            dotProduct += v1.get(i) * v2.get(i);
            normA += Math.pow(v1.get(i), 2);
            normB += Math.pow(v2.get(i), 2);
        }
        if (normA == 0 || normB == 0) {
            return 0.0;
        }
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private static class VectorEntry {
        final String id;
        final String text;
        final Map<String, Object> metadata;
        final List<Double> vector;

        VectorEntry(String id, String text, Map<String, Object> metadata, List<Double> vector) {
            this.id = id;
            this.text = text;
            this.metadata = metadata;
            this.vector = vector;
        }
    }
}
