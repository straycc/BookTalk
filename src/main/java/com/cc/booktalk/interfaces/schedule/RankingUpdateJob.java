package com.cc.booktalk.interfaces.schedule;
import com.cc.booktalk.application.user.service.rank.RankingRefreshService;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import javax.annotation.Resource;


/**
 * 热度榜单定时更新任务
 * 负责定期更新书籍榜单和热门书评榜单到Redis缓存
 *
 * @author cc
 * @since 2025-10-27
 */
@Component
public class RankingUpdateJob {

    @Resource
    private RankingRefreshService rankingRefreshService;

    @XxlJob("calculateBookHotScores")
    @Scheduled(cron = "0 0 */1 * * ?")
    public void calculateBookHotScores() {
        rankingRefreshService.calculateBookHotScores();
    }

    @Scheduled(cron = "0 */30 * * * ?")
    public void calculateReviewHotScores() {
        rankingRefreshService.calculateReviewHotScores();
    }

    @Scheduled(cron = "0 0 */2 * * ?")
    public void updateWeeklyHotBooksToRedis() {
        rankingRefreshService.updateWeeklyHotBooksToRedis();
    }

    @Scheduled(cron = "0 */30 * * * ?")
    public void updateWeeklyHotReviewsToRedis() {
        rankingRefreshService.updateWeeklyHotReviewsToRedis();
    }
}
