package com.lyra.agent.memory;

import com.lyra.agent.llm.EmbeddingModel;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

public class VectorStoreTest {

    @Test
    public void testVectorSearch() {
        // 1. Mock EmbeddingModel
        EmbeddingModel embeddingModel = Mockito.mock(EmbeddingModel.class);
        
        // Setup mock embeddings
        // "apple" -> [1.0, 0.0]
        // "banana" -> [0.9, 0.1]
        // "car" -> [0.0, 1.0]
        when(embeddingModel.embed("apple")).thenReturn(Arrays.asList(1.0, 0.0));
        when(embeddingModel.embed("banana")).thenReturn(Arrays.asList(0.9, 0.1));
        when(embeddingModel.embed("car")).thenReturn(Arrays.asList(0.0, 1.0));
        when(embeddingModel.embed("fruit")).thenReturn(Arrays.asList(1.0, 0.0)); // Query close to apple

        // 2. Create VectorStore
        VectorStore store = new InMemoryVectorStore(embeddingModel);

        // 3. Insert Data
        store.upsert("1", "apple", new HashMap<>());
        store.upsert("2", "banana", new HashMap<>());
        store.upsert("3", "car", new HashMap<>());

        // 4. Search
        List<VectorSearchResult> results = store.similaritySearch("fruit", 2);

        // 5. Verify
        assertEquals(2, results.size());
        assertEquals("apple", results.get(0).getText());
        assertEquals("banana", results.get(1).getText());
        
        // Verify scores (cosine similarity)
        // fruit(1,0) . apple(1,0) = 1.0
        assertEquals(1.0, results.get(0).getScore(), 0.001);
    }
}
