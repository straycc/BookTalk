package com.cc.talkserver.schedule;

import com.cc.talkpojo.enums.RankingPeriod;
import com.cc.talkpojo.enums.RankingType;
import com.cc.talkserver.user.service.RankingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 榜单更新定时任务
 *
 * @author cc
 * @since 2025-10-13
 */
@Slf4j
@Component
public class RankingUpdateTask {

    @Autowired
    private RankingService rankingService;

    /**
     * 每小时更新热门书评榜单
     */
    @Scheduled(cron = "0 0 * * * ?") // 每小时的第0分0秒执行
    public void updateHotReviewsRanking() {
        log.info("开始执行热门书评榜单更新任务");

        try {
            // 更新不同时间周期的榜单
            rankingService.updateHotReviewsRanking(RankingPeriod.DAILY.getCode());
            rankingService.updateHotReviewsRanking(RankingPeriod.WEEKLY.getCode());
            rankingService.updateHotReviewsRanking(RankingPeriod.MONTHLY.getCode());

            log.info("热门书评榜单更新完成");
        } catch (Exception e) {
            log.error("热门书评榜单更新失败", e);
        }
    }

    /**
     * 每天凌晨1点更新书籍榜单
     */
    @Scheduled(cron = "0 0 1 * * ?") // 每天凌晨1点执行
    public void updateBookRanking() {
        log.info("开始执行书籍榜单更新任务");

        try {
            // 更新不同类型的榜单
            rankingService.updateBookRanking(RankingType.BOOK_RATING.getCode(), RankingPeriod.WEEKLY.getCode());
            rankingService.updateBookRanking(RankingType.BOOK_RATING.getCode(), RankingPeriod.MONTHLY.getCode());
            rankingService.updateBookRanking(RankingType.HOT_DISCUSSION.getCode(), RankingPeriod.WEEKLY.getCode());
            rankingService.updateBookRanking(RankingType.HOT_DISCUSSION.getCode(), RankingPeriod.MONTHLY.getCode());
            rankingService.updateBookRanking(RankingType.NEW_BOOKS.getCode(), RankingPeriod.WEEKLY.getCode());
            rankingService.updateBookRanking(RankingType.NEW_BOOKS.getCode(), RankingPeriod.MONTHLY.getCode());

            log.info("书籍榜单更新完成");
        } catch (Exception e) {
            log.error("书籍榜单更新失败", e);
        }
    }

    /**
     * 每10分钟更新一次总榜（保持相对实时）
     */
    @Scheduled(cron = "0 */10 * * * ?") // 每10分钟执行一次
    public void updateAllTimeRanking() {
        log.debug("开始执行总榜更新任务");

        try {
            // 更新总榜
            rankingService.updateHotReviewsRanking(RankingPeriod.ALL_TIME.getCode());
            rankingService.updateBookRanking(RankingType.BOOK_RATING.getCode(), RankingPeriod.ALL_TIME.getCode());
            rankingService.updateBookRanking(RankingType.HOT_DISCUSSION.getCode(), RankingPeriod.ALL_TIME.getCode());

            log.debug("总榜更新完成");
        } catch (Exception e) {
            log.error("总榜更新失败", e);
        }
    }

    /**
     * 手动触发榜单更新（用于测试或紧急更新）
     */
    public void manualUpdateAllRankings() {
        log.info("手动触发榜单更新");

        try {
            // 更新热门书评榜单
            rankingService.updateHotReviewsRanking(RankingPeriod.DAILY.getCode());
            rankingService.updateHotReviewsRanking(RankingPeriod.WEEKLY.getCode());
            rankingService.updateHotReviewsRanking(RankingPeriod.MONTHLY.getCode());
            rankingService.updateHotReviewsRanking(RankingPeriod.ALL_TIME.getCode());

            // 更新书籍榜单
            rankingService.updateBookRanking(RankingType.BOOK_RATING.getCode(), RankingPeriod.WEEKLY.getCode());
            rankingService.updateBookRanking(RankingType.BOOK_RATING.getCode(), RankingPeriod.MONTHLY.getCode());
            rankingService.updateBookRanking(RankingType.HOT_DISCUSSION.getCode(), RankingPeriod.WEEKLY.getCode());
            rankingService.updateBookRanking(RankingType.HOT_DISCUSSION.getCode(), RankingPeriod.MONTHLY.getCode());
            rankingService.updateBookRanking(RankingType.NEW_BOOKS.getCode(), RankingPeriod.WEEKLY.getCode());
            rankingService.updateBookRanking(RankingType.NEW_BOOKS.getCode(), RankingPeriod.MONTHLY.getCode());

            log.info("手动榜单更新完成");
        } catch (Exception e) {
            log.error("手动榜单更新失败", e);
        }
    }
}