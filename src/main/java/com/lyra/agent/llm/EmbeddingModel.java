package com.lyra.agent.llm;

import java.util.List;

/**
 * 嵌入模型接口。
 * 将文本转换为向量表示。
 */
public interface EmbeddingModel {
    /**
     * 生成文本嵌入向量。
     *
     * @param text 输入文本
     * @return 浮点数向量
     */
    List<Double> embed(String text);
}
