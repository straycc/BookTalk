package com.cc.booktalk.interfaces.vo.user.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * AI WebSocket 出站消息。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiRecommendationResponseVO {

    private String type;

    private String sessionId;

    private String answer;

    @Builder.Default
    private List<AiRecommendedBookVO> books = new ArrayList<>();

    @Builder.Default
    private List<String> followUpSuggestions = new ArrayList<>();

    /**
     * 推荐阶段：INIT / NEED_CLARIFY / INTENT_READY / RECOMMENDED。
     */
    private String phase;

    /**
     * 是否为“换一批”结果。
     */
    private Boolean isNewBatch;

    private String error;
}
