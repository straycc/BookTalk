package com.cc.booktalk.post;

import com.cc.booktalk.domain.entity.recommendation.UserBehaviorLog;
import com.cc.booktalk.domain.post.PostHotScoreDomainService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

public class PostHotScoreDomainServiceTest {

    private final PostHotScoreDomainService postHotScoreDomainService = new PostHotScoreDomainService();

    @Test
    public void shouldReturnZeroWhenNoBehaviors() {
        double score = postHotScoreDomainService.calculateHotScore(List.of(), LocalDateTime.now());
        Assertions.assertEquals(0.0, score);
    }

    @Test
    public void shouldGiveHigherScoreToRecentStrongInteraction() {
        LocalDateTime now = LocalDateTime.now();
        UserBehaviorLog recentComment = UserBehaviorLog.builder()
                .behaviorType("POST_COMMENT")
                .createTime(now.minusHours(2))
                .build();
        UserBehaviorLog oldView = UserBehaviorLog.builder()
                .behaviorType("POST_VIEW")
                .createTime(now.minusDays(10))
                .build();

        double recentScore = postHotScoreDomainService.calculateHotScore(List.of(recentComment), now);
        double oldScore = postHotScoreDomainService.calculateHotScore(List.of(oldView), now);

        Assertions.assertTrue(recentScore > oldScore);
    }
}
