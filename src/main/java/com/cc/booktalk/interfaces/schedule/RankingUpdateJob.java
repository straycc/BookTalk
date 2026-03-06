package com.cc.booktalk.interfaces.schedule;
import com.cc.booktalk.application.user.service.rank.BookRankingRefreshService;
import com.cc.booktalk.application.user.service.rank.ReviewRankingRefreshService;
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
    private BookRankingRefreshService bookRankingRefreshService;

    @Resource
    private ReviewRankingRefreshService reviewRankingRefreshService;

    @XxlJob("calculateBookHotScores")
    @Scheduled(cron = "0 0 */2 * * ?")
    public void calculateBookHotScores() {
        bookRankingRefreshService.calculateBookHotScores();
    }

    @Scheduled(cron = "0 0 * * * ?")
    public void calculateReviewHotScores() {
        reviewRankingRefreshService.calculateReviewHotScores();
    }

    @Scheduled(cron = "0 5 */2 * * ?")
    public void refreshHotBooksRanking() {
        bookRankingRefreshService.refreshHotBooksRanking();
    }

    @Scheduled(cron = "0 15 */2 * * ?")
    public void refreshBookRatingRanking() {
        bookRankingRefreshService.refreshBookRatingRanking();
    }

    @Scheduled(cron = "0 25 */2 * * ?")
    public void refreshNewBooksRanking() {
        bookRankingRefreshService.refreshNewBooksRanking();
    }

    @Scheduled(cron = "0 5 * * * ?")
    public void updateHotReviewsToRedis() {
        reviewRankingRefreshService.refreshHotReviewsRanking();
    }
}
