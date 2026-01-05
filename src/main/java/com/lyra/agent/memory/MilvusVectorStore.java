package com.lyra.agent.memory;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.lyra.agent.autoconfigure.LyraAgentProperties;
import com.lyra.agent.llm.EmbeddingModel;
import io.milvus.client.MilvusServiceClient;
import io.milvus.grpc.DataType;
import io.milvus.grpc.MutationResult;
import io.milvus.grpc.SearchResults;
import io.milvus.param.ConnectParam;
import io.milvus.param.MetricType;
import io.milvus.param.R;
import io.milvus.param.collection.CreateCollectionParam;
import io.milvus.param.collection.FieldType;
import io.milvus.param.collection.HasCollectionParam;
import io.milvus.param.collection.LoadCollectionParam;
import io.milvus.param.dml.InsertParam;
import io.milvus.param.dml.SearchParam;
import io.milvus.response.SearchResultsWrapper;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Milvus 向量存储实现。
 */
public class MilvusVectorStore implements VectorStore {
    private static final String ID_FIELD = "id";
    private static final String TEXT_FIELD = "text";
    private static final String METADATA_FIELD = "metadata";
    private static final String VECTOR_FIELD = "vector";

    private final EmbeddingModel embeddingModel;
    private final MilvusServiceClient client;
    private final String collectionName;
    private final Gson gson = new Gson();

    public MilvusVectorStore(EmbeddingModel embeddingModel, LyraAgentProperties.Vector.Milvus config) {
        this.embeddingModel = embeddingModel;
        this.collectionName = config.getCollectionName();
        
        // 1. Connect
        ConnectParam.Builder connectBuilder = ConnectParam.newBuilder()
                .withHost(config.getHost())
                .withPort(config.getPort());
        
        if (config.getUsername() != null && !config.getUsername().isEmpty()) {
            connectBuilder.withAuthorization(config.getUsername(), config.getPassword());
        }
        this.client = new MilvusServiceClient(connectBuilder.build());

        // 2. Init Collection
        initCollection(config.getDimension());
    }

    private void initCollection(int dimension) {
        R<Boolean> hasColl = client.hasCollection(HasCollectionParam.newBuilder()
                .withCollectionName(collectionName)
                .build());
        
        if (hasColl.getData() == Boolean.FALSE) {
            FieldType idField = FieldType.newBuilder()
                    .withName(ID_FIELD)
                    .withDataType(DataType.VarChar)
                    .withMaxLength(256)
                    .withPrimaryKey(true)
                    .withAutoID(false)
                    .build();
            
            FieldType textField = FieldType.newBuilder()
                    .withName(TEXT_FIELD)
                    .withDataType(DataType.VarChar)
                    .withMaxLength(65535)
                    .build();

            FieldType metaField = FieldType.newBuilder()
                    .withName(METADATA_FIELD)
                    .withDataType(DataType.JSON)
                    .build();

            FieldType vectorField = FieldType.newBuilder()
                    .withName(VECTOR_FIELD)
                    .withDataType(DataType.FloatVector)
                    .withDimension(dimension)
                    .build();

            CreateCollectionParam createParam = CreateCollectionParam.newBuilder()
                    .withCollectionName(collectionName)
                    .addFieldType(idField)
                    .addFieldType(textField)
                    .addFieldType(metaField)
                    .addFieldType(vectorField)
                    .build();

            client.createCollection(createParam);
        }
        
        // Ensure loaded
        client.loadCollection(LoadCollectionParam.newBuilder()
                .withCollectionName(collectionName)
                .build());
    }

    @Override
    public void upsert(String id, String text, Map<String, Object> metadata) {
        List<Double> vector = embeddingModel.embed(text);
        List<Float> floatVector = vector.stream().map(Double::floatValue).collect(Collectors.toList());
        
        // Use InsertParam (Note: upsert is supported in newer Milvus versions, here using insert for compatibility)
        // In production, might need delete then insert if ID exists, or use UpsertParam if available in SDK 2.4+
        
        List<InsertParam.Field> fields = new ArrayList<>();
        fields.add(new InsertParam.Field(ID_FIELD, Collections.singletonList(id)));
        fields.add(new InsertParam.Field(TEXT_FIELD, Collections.singletonList(text)));
        fields.add(new InsertParam.Field(METADATA_FIELD, Collections.singletonList(gson.toJsonTree(metadata).getAsJsonObject())));
        fields.add(new InsertParam.Field(VECTOR_FIELD, Collections.singletonList(floatVector)));

        InsertParam insertParam = InsertParam.newBuilder()
                .withCollectionName(collectionName)
                .withFields(fields)
                .build();

        client.insert(insertParam);
    }

    @Override
    public List<VectorSearchResult> similaritySearch(String text, int k) {
        List<Double> vector = embeddingModel.embed(text);
        List<Float> floatVector = vector.stream().map(Double::floatValue).collect(Collectors.toList());

        SearchParam searchParam = SearchParam.newBuilder()
                .withCollectionName(collectionName)
                .withMetricType(MetricType.COSINE)
                .withTopK(k)
                .withVectors(Collections.singletonList(floatVector))
                .withVectorFieldName(VECTOR_FIELD)
                .addOutField(ID_FIELD)
                .addOutField(TEXT_FIELD)
                .addOutField(METADATA_FIELD)
                .build();

        R<SearchResults> response = client.search(searchParam);
        
        if (response.getStatus() != R.Status.Success.getCode()) {
            throw new RuntimeException("Search failed: " + response.getMessage());
        }

        SearchResultsWrapper wrapper = new SearchResultsWrapper(response.getData().getResults());
        List<SearchResultsWrapper.IDScore> scores = wrapper.getIDScore(0);
        
        List<VectorSearchResult> results = new ArrayList<>();
        for (SearchResultsWrapper.IDScore score : scores) {
            String id = (String) score.get(ID_FIELD);
            String docText = (String) score.get(TEXT_FIELD);
            JsonObject metaJson = (JsonObject) score.get(METADATA_FIELD);
            Map<String, Object> metadata = gson.fromJson(metaJson, Map.class);
            
            results.add(new VectorSearchResult(id, docText, metadata, score.getScore()));
        }
        
        return results;
    }
}
