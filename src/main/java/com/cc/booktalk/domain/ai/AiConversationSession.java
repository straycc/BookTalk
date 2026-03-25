package com.cc.booktalk.domain.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * AI 会话状态。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiConversationSession {

    private String sessionId;

    private Long userId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private AiParsedIntent lastIntent;

    @Builder.Default
    private List<AiRecommendedBook> lastBooks = new ArrayList<>();

    @Builder.Default
    private List<AiConversationTurn> turns = new ArrayList<>();
}
