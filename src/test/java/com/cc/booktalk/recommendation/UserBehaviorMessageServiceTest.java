package com.cc.booktalk.recommendation;

import com.cc.booktalk.application.user.service.recommendation.RecommendationService;
import com.cc.booktalk.application.user.service.recommendation.behavior.UserBehaviorMessageService;
import com.cc.booktalk.application.user.service.recommendation.behavior.UserBehaviorService;
import com.cc.booktalk.application.user.service.recommendation.profile.UserBehaviorInterestService;
import com.cc.booktalk.common.event.behavior.UserBehaviorEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserBehaviorMessageServiceTest {

    @InjectMocks private UserBehaviorMessageService messageService;
    @Mock private UserBehaviorService userBehaviorService;
    @Mock private UserBehaviorInterestService userBehaviorInterestService;
    @Mock private RecommendationService recommendationService;

    @Test
    void processedBehaviorInvalidatesUserRecommendationCache() {
        UserBehaviorEvent event = UserBehaviorEvent.builder()
                .userId(1L)
                .targetId(10L)
                .targetType("BOOK")
                .behaviorType("BOOK_VIEW")
                .build();

        messageService.processUserBehavior(event);

        verify(userBehaviorService).recordUserBehavior(event);
        verify(userBehaviorInterestService).updateUserInterest(event);
        verify(recommendationService).clearRecommendationCache(1L);
    }
}
