package com.cc.booktalk.interfaces.schedule;

import com.cc.booktalk.application.user.service.recommendation.RecommendationCacheRefreshService;
import com.xxl.job.core.handler.annotation.XxlJob;
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
    private RecommendationCacheRefreshService recommendationCacheRefreshService;

    @XxlJob("updateActivateUserRec")
    @Scheduled(cron = "0 0 3 * * ?")
    public void updateActivateUserRecommendations() {
        recommendationCacheRefreshService.updateActivateUserRecommendations();
    }


    /**
     * 定时刷新热门图书推荐缓存
     */
    @XxlJob("updateHotRec")
    @Scheduled(cron = "0 0 */6 * * ?")
    public void updateHotRecommendations() {
        recommendationCacheRefreshService.updateHotRecommendations();
    }
}
