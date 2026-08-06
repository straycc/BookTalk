package com.cc.booktalk.application.user.service.ai;

import com.cc.booktalk.domain.ai.AiConversationSession;
import com.cc.booktalk.domain.ai.AiConversationTurn;
import com.cc.booktalk.domain.ai.AiParsedIntent;
import com.cc.booktalk.domain.ai.AiRecommendedBook;

import java.util.List;

/**
 * AI 会话服务。
 */
public interface AiConversationService {

    AiConversationSession loadSession(Long userId, String sessionId);

    void appendTurn(String sessionId, Long userId, AiConversationTurn turn);

    void updateContext(String sessionId, Long userId, AiParsedIntent intent, List<AiRecommendedBook> books);

    void updateRecommendationState(String sessionId,
                                   Long userId,
                                   String phase,
                                   String intentDigest,
                                   String clarifyQuestion,
                                   List<Long> candidateBookIds,
                                   List<Long> shownBookIds);

    void resetSession(String sessionId, Long userId);

    String summarizeConversation(AiConversationSession session);
}
