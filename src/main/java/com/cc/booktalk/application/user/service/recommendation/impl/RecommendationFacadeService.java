package com.cc.booktalk.application.user.service.recommendation.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cc.booktalk.application.user.service.rank.BookRankingRefreshService;
import com.cc.booktalk.application.user.service.recommendation.*;
import com.cc.booktalk.application.user.service.recommendation.model.RecommendationBookCandidate;
import com.cc.booktalk.application.user.service.recommendation.model.UserProfileSnapshot;
import com.cc.booktalk.common.constant.RedisCacheConstant;
import com.cc.booktalk.common.utils.TimeUtils;
import com.cc.booktalk.domain.entity.book.Book;
import com.cc.booktalk.domain.entity.review.BookReview;
import com.cc.booktalk.domain.entity.recommendation.UserBehaviorLog;
import com.cc.booktalk.domain.entity.user.UserInfo;
import com.cc.booktalk.domain.recommendation.HotReviewRecDomain;
import com.cc.booktalk.infrastructure.persistence.user.mapper.book.BookUserMapper;
import com.cc.booktalk.infrastructure.persistence.user.mapper.recommendation.UserBehaviorLogMapper;
import com.cc.booktalk.infrastructure.persistence.user.mapper.recommendation.UserInfoUserMapper;
import com.cc.booktalk.infrastructure.persistence.user.mapper.review.ReviewUserMapper;
import com.cc.booktalk.interfaces.vo.user.rec.PersonalizedRecVO;
import com.cc.booktalk.interfaces.vo.user.review.HotReviewVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RecommendationFacadeService implements RecommendationService {

    private static final int DEFAULT_RECOMMENDATION_COUNT = 10;
    private static final int DEFAULT_HOT_REVIEW_LIMIT = 6;

    @Resource
    private UserProfileService userProfileService;

    @Resource
    private RecommendationRecallService recommendationRecallService;

    @Resource
    private RecommendationFilterService recommendationFilterService;

    @Resource
    private RecommendationRankService recommendationRankService;

    @Resource
    private RecommendationExplainService recommendationExplainService;

    @Resource
    private HotRecommendationService hotRecommendationService;

    @Resource
    private ReviewUserMapper reviewUserMapper;

    @Resource
    private UserInfoUserMapper userInfoUserMapper;

    @Resource
    private BookUserMapper bookUserMapper;

    @Resource
    private UserBehaviorLogMapper userBehaviorLogMapper;

    @Resource
    private HotReviewRecDomain hotReviewRecDomain;

    @Resource(name = "customObjectRedisTemplate")
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    @SuppressWarnings("unchecked")
    public List<PersonalizedRecVO> getPersonalizedRecommendations(Long userId, Integer limit) {
        int finalLimit = limit != null && limit > 0 ? limit : DEFAULT_RECOMMENDATION_COUNT;
        String cacheKey = buildRecommendationCacheKey(userId);
        List<PersonalizedRecVO> cachedResults = (List<PersonalizedRecVO>) redisTemplate.opsForValue().get(cacheKey);
        if (cachedResults != null && !cachedResults.isEmpty()) {
            return cachedResults.stream().limit(finalLimit).collect(Collectors.toList());
        }

        UserProfileSnapshot snapshot = userProfileService.buildSnapshot(userId);
        if (snapshot.isEmpty()) {
            return hotRecommendationService.getHotRecommendations(finalLimit);
        }

        List<RecommendationBookCandidate> candidates = recommendationRecallService.recallCandidates(snapshot, finalLimit);
        candidates = recommendationFilterService.filterCandidates(userId, candidates);
        if (candidates.isEmpty()) {
            return hotRecommendationService.getHotRecommendations(finalLimit);
        }

        List<RecommendationBookCandidate> rankedCandidates = recommendationRankService.rankCandidates(candidates, finalLimit);
        for (RecommendationBookCandidate candidate : rankedCandidates) {
            candidate.setRecommendTime(LocalDateTime.now());
        }
        List<PersonalizedRecVO> recommendations = recommendationExplainService.buildRecommendations(rankedCandidates);
        if (recommendations.isEmpty()) {
            return hotRecommendationService.getHotRecommendations(finalLimit);
        }

        redisTemplate.opsForValue().set(cacheKey, recommendations, Duration.ofHours(12));
        return recommendations;
    }

    @Override
    public List<PersonalizedRecVO> getHotRecommendations(Integer limit) {
        return hotRecommendationService.getHotRecommendations(limit);
    }

    @Override
    public List<PersonalizedRecVO> refreshHotRecommendationsCache(Integer limit) {
        return hotRecommendationService.refreshHotRecommendationsCache(limit);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<HotReviewVO> getHotReviewRecommendations(String period, Integer limit) {
        int finalLimit = (limit != null && limit > 0) ? limit : DEFAULT_HOT_REVIEW_LIMIT;
        String normalizedPeriod = normalizeReviewPeriod(period);
        String cacheKey = RedisCacheConstant.RECOMMENDATIONS_HOT_REVIEWS_PREFIX + normalizedPeriod;

        List<HotReviewVO> cached = (List<HotReviewVO>) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null && !cached.isEmpty()) {
            return cached.stream().limit(finalLimit).collect(Collectors.toList());
        }

        List<HotReviewVO> hotReviews = refreshHotReviewRecommendationsCache(normalizedPeriod, Math.max(finalLimit, 20));
        return hotReviews.stream().limit(finalLimit).collect(Collectors.toList());
    }

    @Override
    public List<HotReviewVO> refreshHotReviewRecommendationsCache(String period, Integer limit) {
        String normalizedPeriod = normalizeReviewPeriod(period);
        int finalLimit = (limit != null && limit > 0) ? limit : 20;
        String cacheKey = RedisCacheConstant.RECOMMENDATIONS_HOT_REVIEWS_PREFIX + normalizedPeriod;
        List<HotReviewVO> hotReviews = calculateHotReviewRecommendations(normalizedPeriod, finalLimit);
        if (!hotReviews.isEmpty()) {
            redisTemplate.opsForValue().set(cacheKey, hotReviews, resolveReviewCacheTtl(normalizedPeriod));
        }
        return hotReviews;
    }

    @Override
    public void clearRecommendationCache(Long userId) {
        String pattern = RedisCacheConstant.RECOMMENDATIONS_PREFIX + userId + ":*";
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    private String buildRecommendationCacheKey(Long userId) {
        return RedisCacheConstant.RECOMMENDATIONS_PREFIX + userId + ":books";
    }

    private List<HotReviewVO> calculateHotReviewRecommendations(String period, int limit) {
        int days = resolveReviewDays(period);
        int candidateLimit = Math.max(limit * 5, 50);
        List<Long> candidateReviewIds = userBehaviorLogMapper.getHotReviewCandidateIds(days, candidateLimit);
        if (candidateReviewIds == null || candidateReviewIds.isEmpty()) {
            return getLatestReviewFallback(limit);
        }

        Map<Long, BookReview> reviewMap = reviewUserMapper.selectBatchIds(candidateReviewIds).stream()
                .collect(Collectors.toMap(BookReview::getId, it -> it, (left, right) -> left));

        LocalDateTime now = LocalDateTime.now();
        List<HotReviewVO> rankedReviews = new ArrayList<>();
        for (Long reviewId : candidateReviewIds) {
            BookReview review = reviewMap.get(reviewId);
            if (review == null) {
                continue;
            }
            List<UserBehaviorLog> behaviors = userBehaviorLogMapper.getReviewRecentBehaviors(reviewId, days);
            if (behaviors == null || !hotReviewRecDomain.enoughActions(behaviors.size())) {
                continue;
            }
            HotReviewVO vo = toHotReviewVO(review);
            vo.setHotScore(hotReviewRecDomain.calculateHotScore(behaviors, now));
            rankedReviews.add(vo);
        }

        rankedReviews.sort((left, right) -> Double.compare(right.getHotScore(), left.getHotScore()));
        List<HotReviewVO> hotReviews = rankedReviews.stream().limit(limit).collect(Collectors.toList());

        if (hotReviews.size() < limit) {
            int need = limit - hotReviews.size();
            List<HotReviewVO> fallbackLatest = getLatestReviewFallback(limit);
            Set<Long> existed = hotReviews.stream().map(HotReviewVO::getReviewId).collect(Collectors.toSet());
            for (HotReviewVO item : fallbackLatest) {
                if (item.getReviewId() == null || existed.contains(item.getReviewId())) {
                    continue;
                }
                hotReviews.add(item);
                existed.add(item.getReviewId());
                if (--need <= 0) {
                    break;
                }
            }
        }
        for (int i = 0; i < hotReviews.size(); i++) {
            hotReviews.get(i).setRank(i + 1);
        }
        return hotReviews;
    }

    private List<HotReviewVO> getLatestReviewFallback(int limit) {
        LambdaQueryWrapper<BookReview> query = new LambdaQueryWrapper<>();
        query.orderByDesc(BookReview::getCreateTime).last("LIMIT " + limit);
        return reviewUserMapper.selectList(query).stream().map(this::toHotReviewVO).collect(Collectors.toList());
    }

    private HotReviewVO toHotReviewVO(BookReview review) {
        HotReviewVO vo = new HotReviewVO();
        vo.setReviewId(review.getId());
        vo.setTitle(review.getTitle());
        vo.setContent(review.getContent());
        vo.setBookId(review.getBookId());
        vo.setLikeCount(review.getLikeCount() == null ? 0L : review.getLikeCount().longValue());
        vo.setCommentCount(review.getReplyCount() == null ? 0L : review.getReplyCount().longValue());
        vo.setHotScore(review.getHotScore());
        vo.setCreateTime(review.getCreateTime());
        vo.setTimeDesc(TimeUtils.getTimeDesc(review.getCreateTime()));

        UserInfo author = userInfoUserMapper.selectById(review.getUserId());
        if (author != null) {
            vo.setAuthor(author.getNickname());
            vo.setAuthorAvatar(author.getAvatarUrl());
        }
        Book book = bookUserMapper.selectById(review.getBookId());
        if (book != null) {
            vo.setBookName(book.getTitle());
            vo.setBookCover(book.getCoverUrl());
            vo.setCategoryId(book.getCategoryId());
        }
        return vo;
    }

    private String normalizeReviewPeriod(String period) {
        if (period == null || period.isBlank()) {
            return "weekly";
        }
        String p = period.trim().toLowerCase();
        switch (p) {
            case "24h":
            case "1d":
            case "day":
            case "daily":
                return "daily";
            case "30d":
            case "month":
            case "monthly":
                return "monthly";
            case "7d":
            case "week":
            case "weekly":
            default:
                return "weekly";
        }
    }

    private int resolveReviewDays(String period) {
        switch (normalizeReviewPeriod(period)) {
            case "daily":
                return 1;
            case "monthly":
                return 30;
            case "weekly":
            default:
                return 7;
        }
    }

    private Duration resolveReviewCacheTtl(String period) {
        switch (period) {
            case "daily":
                return Duration.ofMinutes(15);
            case "monthly":
                return Duration.ofHours(2);
            case "weekly":
            default:
                return Duration.ofHours(1);
        }
    }
}
