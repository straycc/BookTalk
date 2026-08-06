package com.cc.booktalk.interfaces.schedule;

import com.cc.booktalk.application.user.service.recommendation.RecommendationService;
import com.cc.booktalk.application.user.service.post.PostHotRefreshService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import javax.annotation.Resource;


/**
 * 个性化推荐定时更新任务
 *
 * @author cc
 * @since 2025-10-27
 */
@Component
public class RecommendationJob {
    @Resource
    private RecommendationService recommendationService;

    @Resource
    private PostHotRefreshService postHotRefreshService;

    @Scheduled(cron = "0 0 */6 * * ?")
    public void updateHotRecommendations() {
        recommendationService.refreshHotRecommendationsCache(50);
        recommendationService.refreshHotReviewRecommendationsCache("daily", 20);
        recommendationService.refreshHotReviewRecommendationsCache("weekly", 20);
    }

    @Scheduled(cron = "0 */30 * * * ?")
    public void refreshPostHotScore() {
        postHotRefreshService.refreshPostHotScores();
    }
}
