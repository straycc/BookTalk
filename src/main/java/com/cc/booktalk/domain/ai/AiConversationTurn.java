package com.cc.booktalk.domain.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * AI 会话单轮消息。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiConversationTurn {

    /**
     * 角色：user / assistant / system。
     */
    private String role;

    /**
     * 文本内容。
     */
    private String content;

    /**
     * 元数据，用于保存解析结果或候选摘要。
     */
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    /**
     * 发生时间。
     */
    private LocalDateTime createdAt;
}
