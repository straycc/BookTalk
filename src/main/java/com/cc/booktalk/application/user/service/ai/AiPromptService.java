package com.cc.booktalk.application.user.service.ai;

import com.cc.booktalk.domain.ai.AiRecommendationContext;
import com.cc.booktalk.infrastructure.ai.model.LlmChatRequest;

/**
 * AI Prompt 服务。
 */
public interface AiPromptService {

    LlmChatRequest buildIntentRequest(String userInput, String conversationSummary);

    LlmChatRequest buildAnswerRequest(AiRecommendationContext context);
}
