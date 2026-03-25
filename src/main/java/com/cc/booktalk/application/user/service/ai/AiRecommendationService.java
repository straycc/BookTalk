package com.cc.booktalk.application.user.service.ai;

import com.cc.booktalk.interfaces.vo.user.ai.AiRecommendationResponseVO;

/**
 * AI 推荐主服务。
 */
public interface AiRecommendationService {

    AiRecommendationResponseVO ask(Long userId, String sessionId, String userInput, String messageType);

    AiRecommendationResponseVO reset(Long userId, String sessionId);
}
