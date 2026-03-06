package com.cc.booktalk.application.user.service.rank;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.cc.booktalk.common.constant.RedisCacheConstant;
import com.cc.booktalk.common.utils.TimeUtils;
import com.cc.booktalk.domain.entity.book.Book;
import com.cc.booktalk.domain.entity.recommendation.UserBehaviorLog;
import com.cc.booktalk.domain.entity.review.BookReview;
import com.cc.booktalk.domain.entity.user.UserInfo;
import com.cc.booktalk.domain.recommendation.HotReviewRecDomain;
import com.cc.booktalk.infrastructure.persistence.user.mapper.book.BookUserMapper;
import com.cc.booktalk.infrastructure.persistence.user.mapper.recommendation.UserBehaviorLogMapper;
import com.cc.booktalk.infrastructure.persistence.user.mapper.recommendation.UserInfoUserMapper;
import com.cc.booktalk.infrastructure.persistence.user.mapper.review.ReviewUserMapper;
import com.cc.booktalk.interfaces.vo.user.review.HotReviewVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class ReviewRankingRefreshService {

    private static final String TARGET_TYPE_REVIEW = "REVIEW";
    private static final String PERIOD = "weekly";
    private static final int WINDOW_DAYS = 7;
    private static final int RANK_LIMIT = 50;
    private static final Duration CACHE_TTL = Duration.ofMinutes(30);

    @Resource
    private BookUserMapper bookUserMapper;

    @Resource
    private ReviewUserMapper reviewUserMapper;

    @Resource
    private UserInfoUserMapper userInfoUserMapper;

    @Resource
    private UserBehaviorLogMapper userBehaviorLogMapper;

    @Resource(name = "customObjectRedisTemplate")
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private HotReviewRecDomain hotReviewRecDomain;

    /**
     * 完整链路：计算热度 -> 入库 -> 刷榜单缓存
     */
    public void refreshHotReviewsRanking() {
        LocalDateTime since = LocalDateTime.now().minusDays(WINDOW_DAYS);
        calculateAndPersistReviewHotScores(since);
        buildAndCacheRanking(RANK_LIMIT);
    }

    public void calculateReviewHotScores() {
        calculateAndPersistReviewHotScores(LocalDateTime.now().minusDays(WINDOW_DAYS));
    }

    public void updateHotReviewsToRedis() {
        buildAndCacheRanking(RANK_LIMIT);
    }

    @Transactional
    private void calculateAndPersistReviewHotScores(LocalDateTime since) {
        try {
            log.info("开始计算书评热度分数");
            LambdaQueryWrapper<BookReview> reviewQuery = new LambdaQueryWrapper<>();
            reviewQuery.ge(BookReview::getCreateTime, since).select(BookReview::getId);
            List<BookReview> reviews = reviewUserMapper.selectList(reviewQuery);
            if (reviews.isEmpty()) {
                log.info("最近{}天无书评，跳过热度计算", WINDOW_DAYS);
                return;
            }

            int updatedCount = 0;
            for (BookReview review : reviews) {
                try {
                    Double hotScore = computeReviewHotScore(review.getId(), since);
                    LambdaUpdateWrapper<BookReview> update = new LambdaUpdateWrapper<>();
                    update.eq(BookReview::getId, review.getId())
                            .set(BookReview::getHotScore, hotScore)
                            .set(BookReview::getHotScoreUpdateTime, LocalDateTime.now());
                    reviewUserMapper.update(null, update);
                    updatedCount++;
                } catch (Exception e) {
                    log.error("计算书评热度失败: reviewId={}", review.getId(), e);
                }
            }
            log.info("书评热度计算完成: 更新数量={}", updatedCount);
        } catch (Exception e) {
            log.error("计算书评热度分数失败", e);
        }
    }

    private Double computeReviewHotScore(Long reviewId, LocalDateTime since) {
        LambdaQueryWrapper<UserBehaviorLog> behaviorQuery = new LambdaQueryWrapper<>();
        behaviorQuery.eq(UserBehaviorLog::getTargetId, reviewId)
                .eq(UserBehaviorLog::getTargetType, TARGET_TYPE_REVIEW)
                .ge(UserBehaviorLog::getCreateTime, since);
        List<UserBehaviorLog> reviewBehaviors = userBehaviorLogMapper.selectList(behaviorQuery);
        return hotReviewRecDomain.calculateHotScore(reviewBehaviors, LocalDateTime.now());
    }

    private void buildAndCacheRanking(int limit) {
        try {
            LambdaQueryWrapper<BookReview> rankingQuery = new LambdaQueryWrapper<>();
            rankingQuery.orderByDesc(BookReview::getHotScore)
                    .orderByDesc(BookReview::getHotScoreUpdateTime)
                    .orderByDesc(BookReview::getCreateTime)
                    .last("LIMIT " + limit);
            List<BookReview> reviews = reviewUserMapper.selectList(rankingQuery);

            List<HotReviewVO> rankings = new ArrayList<>(reviews.size());
            for (int i = 0; i < reviews.size(); i++) {
                rankings.add(toHotReviewVO(reviews.get(i), i + 1));
            }

            String redisKey = RedisCacheConstant.RANKING_HOT_REVIEWS_PREFIX + PERIOD;
            redisTemplate.opsForValue().set(redisKey, rankings, CACHE_TTL);
            log.info("热门书评榜单缓存更新完成: key={}, count={}", redisKey, rankings.size());
        } catch (Exception e) {
            log.error("更新热门书评榜单缓存失败", e);
        }
    }

    private HotReviewVO toHotReviewVO(BookReview review, int rank) {
        HotReviewVO vo = new HotReviewVO();
        BeanUtils.copyProperties(review, vo);
        vo.setReviewId(review.getId());
        vo.setBookId(review.getBookId());
        vo.setContent(review.getContent());
        vo.setHotScore(review.getHotScore());
        vo.setRank(rank);
        vo.setTimeDesc(TimeUtils.getTimeDesc(review.getCreateTime()));

        UserInfo userInfo = userInfoUserMapper.selectById(review.getUserId());
        if (userInfo != null) {
            vo.setAuthor(userInfo.getNickname());
            vo.setAuthorAvatar(userInfo.getAvatarUrl());
        }

        Book book = bookUserMapper.selectById(review.getBookId());
        if (book != null) {
            vo.setBookName(book.getTitle());
            vo.setBookCover(book.getCoverUrl());
            vo.setCategoryId(book.getCategoryId());
        }
        return vo;
    }
}

