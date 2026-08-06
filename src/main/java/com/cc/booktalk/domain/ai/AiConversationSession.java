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

    /**
     * 推荐阶段：INIT / NEED_CLARIFY / INTENT_READY / RECOMMENDED。
     */
    private String recommendationPhase;

    /**
     * 最近一次意图摘要，作为会话候选缓存命中判断依据。
     */
    private String intentDigest;

    /**
     * 最近一次追问文案。
     */
    private String clarifyQuestion;

    /**
     * 当前会话候选书 ID（轻量缓存，避免重复查库）。
     */
    @Builder.Default
    private List<Long> candidateBookIds = new ArrayList<>();

    /**
     * 当前会话已展示书 ID（用于去重）。
     */
    @Builder.Default
    private List<Long> shownBookIds = new ArrayList<>();
}
