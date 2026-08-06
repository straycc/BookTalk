package com.cc.booktalk.ai;

import com.cc.booktalk.application.user.service.ai.AiConversationService;
import com.cc.booktalk.application.user.service.ai.AiPromptService;
import com.cc.booktalk.application.user.service.ai.impl.AiRecommendationServiceImpl;
import com.cc.booktalk.application.user.service.book.BookUserService;
import com.cc.booktalk.application.user.service.recommendation.RecommendationService;
import com.cc.booktalk.domain.ai.AiConversationSession;
import com.cc.booktalk.infrastructure.ai.LlmClient;
import com.cc.booktalk.infrastructure.persistence.user.mapper.book.BookUserMapper;
import com.cc.booktalk.infrastructure.persistence.user.mapper.recommendation.UserInterestTagMapper;
import com.cc.booktalk.interfaces.vo.user.ai.AiRecommendationResponseVO;
import com.cc.booktalk.interfaces.vo.user.rec.PersonalizedRecVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AiRecommendationServiceImplTest {

    @Test
    public void shouldFallbackWhenLlmUnavailable() {
        AiRecommendationServiceImpl service = new AiRecommendationServiceImpl();

        AiConversationService conversationService = mock(AiConversationService.class);
        AiPromptService promptService = mock(AiPromptService.class);
        RecommendationService recommendationService = mock(RecommendationService.class);
        BookUserService bookUserService = mock(BookUserService.class);
        LlmClient llmClient = mock(LlmClient.class);
        UserInterestTagMapper userInterestTagMapper = mock(UserInterestTagMapper.class);
        BookUserMapper bookUserMapper = mock(BookUserMapper.class);

        org.springframework.test.util.ReflectionTestUtils.setField(service, "aiConversationService", conversationService);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "aiPromptService", promptService);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "recommendationService", recommendationService);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "bookUserService", bookUserService);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "llmClient", llmClient);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "userInterestTagMapper", userInterestTagMapper);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "bookUserMapper", bookUserMapper);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "objectMapper", new ObjectMapper());

        when(conversationService.loadSession(ArgumentMatchers.eq(1L), ArgumentMatchers.isNull()))
                .thenReturn(AiConversationSession.builder()
                        .sessionId("s1")
                        .userId(1L)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build());
        when(conversationService.summarizeConversation(ArgumentMatchers.any())).thenReturn("");
        when(llmClient.isAvailable()).thenReturn(false);
        when(recommendationService.getPersonalizedRecommendations(ArgumentMatchers.eq(1L), ArgumentMatchers.anyInt())).thenReturn(List.of(
                PersonalizedRecVO.builder()
                        .bookId(1L)
                        .bookTitle("终身成长")
                        .author("卡罗尔")
                        .score(88D)
                        .reason("命中成长标签")
                        .confidence(0.8D)
                        .build()
        ));
        when(recommendationService.getHotRecommendations(ArgumentMatchers.anyInt())).thenReturn(List.of());

        AiRecommendationResponseVO response = service.ask(1L, null, "给我推荐成长向作品", "ASK_RECOMMENDATION");

        Assertions.assertEquals("AI_RECOMMENDATION_RESULT", response.getType());
        Assertions.assertFalse(response.getBooks().isEmpty());
        Assertions.assertTrue(response.getAnswer().contains("站内书库"));
    }
}
