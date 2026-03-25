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

    private String error;
}
