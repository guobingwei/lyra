package com.lyra.agent.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lyra.agent.agent.ParsedAction;
import java.util.Map;
import java.util.Collections;

/**
 * 将 LLM 文本输出解析为结构化动作。
 * 期望格式包含 "Final Answer:" 或包含 "Action:" 与 "Input:" 两段。
 */
public class ReActOutputParser {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 解析文本，返回最终答案或下一步动作。
     *
     * @param text LLM 输出文本
     * @return 解析后的动作对象
     */
    public static ParsedAction parse(String text) {
        String lt = text == null ? "" : text.trim();
        int fi = lt.indexOf("Final Answer:");
        if (fi >= 0) {
            String ans = lt.substring(fi + "Final Answer:".length()).trim();
            return new ParsedAction(ans);
        }
        int ai = lt.indexOf("Action:");
        int ii = lt.indexOf("Input:");
        
        if (ai < 0 || ii < 0) {
             // 如果无法解析出动作，且文本不为空，将其视为最终答案
             if (!lt.isEmpty()) {
                 return new ParsedAction(lt);
             }
             // 文本为空或无法处理的情况
             return new ParsedAction("thought", "unknown", Collections.emptyMap());
        }

        String action = lt.substring(ai + "Action:".length(), ii).trim();
        String inputJson = lt.substring(ii + "Input:".length()).trim();
        
        Map<String, Object> input;
        try {
            input = objectMapper.readValue(inputJson, Map.class);
        } catch (Exception e) {
            input = Collections.emptyMap();
        }
        return new ParsedAction("thought", action, input);
    }
}