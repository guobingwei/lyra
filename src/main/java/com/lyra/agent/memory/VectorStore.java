package com.lyra.agent.memory;

import java.util.List;
import java.util.Map;

/**
 * 向量存储接口。
 * 支持文本的向量化存储与相似度检索。
 */
public interface VectorStore {
    /**
     * 插入或更新向量记录。
     *
     * @param id       唯一标识
     * @param text     文本内容
     * @param metadata 元数据
     */
    void upsert(String id, String text, Map<String, Object> metadata);

    /**
     * 相似度搜索。
     *
     * @param text 查询文本
     * @param k    返回结果数量
     * @return 匹配结果列表
     */
    List<VectorSearchResult> similaritySearch(String text, int k);
}
