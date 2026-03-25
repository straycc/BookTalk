package com.cc.booktalk.application.user.service.ai.impl;

import com.cc.booktalk.application.user.service.ai.AiConversationService;
import com.cc.booktalk.common.ai.AiProperties;
import com.cc.booktalk.common.constant.RedisCacheConstant;
import com.cc.booktalk.domain.ai.AiConversationSession;
import com.cc.booktalk.domain.ai.AiConversationTurn;
import com.cc.booktalk.domain.ai.AiParsedIntent;
import com.cc.booktalk.domain.ai.AiRecommendedBook;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AI 会话服务实现。
 */
@Service
public class AiConversationServiceImpl implements AiConversationService {

    @Resource
    private AiProperties aiProperties;

    @Resource(name = "customObjectRedisTemplate")
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public AiConversationSession loadSession(Long userId, String sessionId) {
        String resolvedSessionId = resolveSessionId(sessionId);
        String key = buildKey(userId, resolvedSessionId);
        Object cached = redisTemplate.opsForValue().get(key);
        if (cached instanceof AiConversationSession) {
            return (AiConversationSession) cached;
        }

        AiConversationSession session = AiConversationSession.builder()
                .sessionId(resolvedSessionId)
                .userId(userId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        save(session);
        return session;
    }

    @Override
    public void appendTurn(String sessionId, Long userId, AiConversationTurn turn) {
        AiConversationSession session = loadSession(userId, sessionId);
        List<AiConversationTurn> turns = session.getTurns();
        if (turns == null) {
            turns = new ArrayList<>();
            session.setTurns(turns);
        }
        turns.add(turn);
        trimTurns(turns);
        session.setUpdatedAt(LocalDateTime.now());
        save(session);
    }

    @Override
    public void updateContext(String sessionId, Long userId, AiParsedIntent intent, List<AiRecommendedBook> books) {
        AiConversationSession session = loadSession(userId, sessionId);
        session.setLastIntent(intent);
        session.setLastBooks(books == null ? new ArrayList<>() : new ArrayList<>(books));
        session.setUpdatedAt(LocalDateTime.now());
        save(session);
    }

    @Override
    public void resetSession(String sessionId, Long userId) {
        redisTemplate.delete(buildKey(userId, sessionId));
    }

    @Override
    public String summarizeConversation(AiConversationSession session) {
        if (session == null) {
            return "";
        }
        List<String> parts = new ArrayList<>();
        if (session.getLastIntent() != null && session.getLastIntent().getQuerySummary() != null) {
            parts.add("上次需求：" + session.getLastIntent().getQuerySummary());
        }
        if (session.getLastBooks() != null && !session.getLastBooks().isEmpty()) {
            String books = session.getLastBooks().stream()
                    .limit(3)
                    .map(AiRecommendedBook::getBookTitle)
                    .collect(Collectors.joining("、"));
            parts.add("上次推荐：" + books);
        }
        if (session.getTurns() != null && !session.getTurns().isEmpty()) {
            String recentTurns = session.getTurns().stream()
                    .skip(Math.max(0, session.getTurns().size() - 4))
                    .map(turn -> turn.getRole() + ":" + turn.getContent())
                    .collect(Collectors.joining(" | "));
            parts.add("最近对话：" + recentTurns);
        }
        return String.join("；", parts);
    }

    private void trimTurns(List<AiConversationTurn> turns) {
        int maxTurns = Math.max(6, aiProperties.getMaxSessionTurns());
        while (turns.size() > maxTurns) {
            turns.remove(0);
        }
    }

    private void save(AiConversationSession session) {
        redisTemplate.opsForValue().set(
                buildKey(session.getUserId(), session.getSessionId()),
                session,
                Duration.ofHours(Math.max(1, aiProperties.getSessionTtlHours()))
        );
    }

    private String buildKey(Long userId, String sessionId) {
        return RedisCacheConstant.AI_CONVERSATION_PREFIX + userId + ":" + sessionId;
    }

    private String resolveSessionId(String sessionId) {
        if (sessionId != null && !sessionId.isBlank()) {
            return sessionId;
        }
        return java.util.UUID.randomUUID().toString().replace("-", "");
    }
}
