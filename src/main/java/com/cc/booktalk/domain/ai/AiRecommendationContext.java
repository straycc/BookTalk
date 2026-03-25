package com.cc.booktalk.domain.ai;

import com.cc.booktalk.interfaces.vo.user.rec.PersonalizedRecVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * AI 推荐上下文。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiRecommendationContext {

    private Long userId;

    private String sessionId;

    private String userInput;

    private String messageType;

    private String conversationSummary;

    private AiParsedIntent parsedIntent;

    @Builder.Default
    private List<PersonalizedRecVO> candidateBooks = new ArrayList<>();
}
