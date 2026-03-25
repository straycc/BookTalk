package com.cc.booktalk.application.user.service.recommendation.impl;

import com.cc.booktalk.application.user.service.rank.BookRankingRefreshService;
import com.cc.booktalk.application.user.service.recommendation.HotRecommendationService;
import com.cc.booktalk.common.constant.RedisCacheConstant;
import com.cc.booktalk.domain.entity.recommendation.UserBehaviorLog;
import com.cc.booktalk.domain.recommendation.HotBookRecDomain;
import com.cc.booktalk.infrastructure.persistence.user.mapper.book.BookUserMapper;
import com.cc.booktalk.infrastructure.persistence.user.mapper.recommendation.UserBehaviorLogMapper;
import com.cc.booktalk.interfaces.vo.user.ranking.BookRankingVO;
import com.cc.booktalk.interfaces.vo.user.rec.PersonalizedRecVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class HotRecommendationServiceImpl implements HotRecommendationService {

    private static final int DEFAULT_RECOMMENDATION_COUNT = 10;
    private static final int HOT_CACHE_PREWARM_COUNT = 50;

    @Resource
    private BookRankingRefreshService bookRankingRefreshService;

    @Resource
    private UserBehaviorLogMapper userBehaviorLogMapper;

    @Resource
    private BookUserMapper bookUserMapper;

    @Resource
    private HotBookRecDomain hotBookRecDomain;

    @Resource(name = "customObjectRedisTemplate")
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public List<PersonalizedRecVO> getHotRecommendations(Integer limit) {
        try {
            int finalLimit = limit != null && limit > 0 ? limit : DEFAULT_RECOMMENDATION_COUNT;
            List<PersonalizedRecVO> hotFromRank = getHotRecommendationsFromRankingCache();
            if (!hotFromRank.isEmpty()) {
                return hotFromRank.stream().limit(finalLimit).collect(Collectors.toList());
            }

            bookRankingRefreshService.refreshHotBooksRanking();
            hotFromRank = getHotRecommendationsFromRankingCache();
            if (!hotFromRank.isEmpty()) {
                return hotFromRank.stream().limit(finalLimit).collect(Collectors.toList());
            }

            List<PersonalizedRecVO> refreshed = refreshHotRecommendationsCache(HOT_CACHE_PREWARM_COUNT);
            return refreshed.stream().limit(finalLimit).collect(Collectors.toList());
        } catch (Exception e) {
            log.error("获取热门推荐失败", e);
            return new ArrayList<>();
        }
    }

    @Override
    public List<PersonalizedRecVO> refreshHotRecommendationsCache(Integer limit) {
        try {
            int finalLimit = limit != null && limit > 0 ? limit : HOT_CACHE_PREWARM_COUNT;
            bookRankingRefreshService.refreshHotBooksRanking();
            List<PersonalizedRecVO> hotFromRank = getHotRecommendationsFromRankingCache();
            if (!hotFromRank.isEmpty()) {
                return hotFromRank.stream().limit(finalLimit).collect(Collectors.toList());
            }

            List<PersonalizedRecVO> hotBooks = calculateHotRecommendations(finalLimit);
            if (!hotBooks.isEmpty()) {
                redisTemplate.opsForValue().set(RedisCacheConstant.RECOMMENDATIONS_HOT, hotBooks, Duration.ofHours(6));
            }
            return hotBooks;
        } catch (Exception e) {
            log.error("刷新热门推荐缓存失败", e);
            return new ArrayList<>();
        }
    }

    private List<PersonalizedRecVO> getHotRecommendationsFromRankingCache() {
        String rankKey = RedisCacheConstant.RANKING_HOT_BOOKS_PREFIX + "weekly";
        Object cacheObj = redisTemplate.opsForValue().get(rankKey);
        if (!(cacheObj instanceof List)) {
            return List.of();
        }
        List<?> rawList = (List<?>) cacheObj;
        if (rawList.isEmpty()) {
            return List.of();
        }
        return rawList.stream()
                .map(this::toRecFromRankingCacheItem)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private PersonalizedRecVO toRecFromRankingCacheItem(Object item) {
        if (item instanceof BookRankingVO) {
            return toRecFromRanking((BookRankingVO) item);
        }
        if (!(item instanceof Map)) {
            return null;
        }
        Map<?, ?> map = (Map<?, ?>) item;
        Long bookId = toLong(map.get("bookId"));
        if (bookId == null) {
            return null;
        }
        PersonalizedRecVO rec = new PersonalizedRecVO();
        rec.setBookId(bookId);
        rec.setBookTitle(toStr(map.get("bookTitle")));
        rec.setAuthor(toStr(map.get("author")));
        rec.setBookCover(toStr(map.get("bookCover")));
        rec.setScore(toDouble(map.get("hotScore")));
        rec.setReason("热门榜单推荐");
        rec.setAlgorithmType("POPULAR");
        rec.setRecommendTime(LocalDateTime.now());
        return rec;
    }

    private PersonalizedRecVO toRecFromRanking(BookRankingVO ranking) {
        PersonalizedRecVO rec = new PersonalizedRecVO();
        rec.setBookId(ranking.getBookId());
        rec.setBookTitle(ranking.getBookTitle());
        rec.setAuthor(ranking.getAuthor());
        rec.setBookCover(ranking.getBookCover());
        rec.setScore(ranking.getHotScore());
        rec.setReason("热门榜单推荐");
        rec.setAlgorithmType("POPULAR");
        rec.setRecommendTime(LocalDateTime.now());
        return rec;
    }

    private List<PersonalizedRecVO> calculateHotRecommendations(int finalLimit) {
        int candidateLimit = Math.max(finalLimit * 5, 50);
        List<Long> candidateBookIds = userBehaviorLogMapper.getHotBookCandidateIds(30, candidateLimit);
        if (candidateBookIds == null || candidateBookIds.isEmpty()) {
            return buildFallbackHotBooks(finalLimit, LocalDateTime.now());
        }

        List<PersonalizedRecVO> candidateBooks = bookUserMapper.getRecBookBaseByIds(candidateBookIds);
        Map<Long, PersonalizedRecVO> bookInfoMap = candidateBooks.stream()
                .collect(Collectors.toMap(PersonalizedRecVO::getBookId, it -> it, (left, right) -> left, LinkedHashMap::new));

        LocalDateTime now = LocalDateTime.now();
        List<PersonalizedRecVO> rankedBooks = new ArrayList<>();
        for (Long bookId : candidateBookIds) {
            PersonalizedRecVO base = bookInfoMap.get(bookId);
            if (base == null) {
                continue;
            }
            List<UserBehaviorLog> behaviors = userBehaviorLogMapper.getBookRecentBehaviors(bookId, 30);
            if (behaviors == null || !hotBookRecDomain.enoughActions(behaviors.size())) {
                continue;
            }
            double score = hotBookRecDomain.calculateHotScore(behaviors, now);
            base.setScore(score);
            base.setReason("近期热度上升");
            base.setAlgorithmType("POPULAR");
            base.setRecommendTime(now);
            rankedBooks.add(base);
        }

        rankedBooks.sort((left, right) -> Double.compare(
                right.getScore() == null ? 0D : right.getScore(),
                left.getScore() == null ? 0D : left.getScore()
        ));

        List<PersonalizedRecVO> hotBooks = rankedBooks.stream().limit(finalLimit).collect(Collectors.toList());
        if (hotBooks.size() < finalLimit) {
            int need = finalLimit - hotBooks.size();
            List<PersonalizedRecVO> fallback = buildFallbackHotBooks(finalLimit, now);
            Set<Long> existed = hotBooks.stream().map(PersonalizedRecVO::getBookId).collect(Collectors.toSet());
            for (PersonalizedRecVO item : fallback) {
                if (item.getBookId() == null || existed.contains(item.getBookId())) {
                    continue;
                }
                hotBooks.add(item);
                existed.add(item.getBookId());
                if (--need <= 0) {
                    break;
                }
            }
        }
        return hotBooks;
    }

    private List<PersonalizedRecVO> buildFallbackHotBooks(int limit, LocalDateTime now) {
        List<PersonalizedRecVO> fallback = bookUserMapper.getFallbackHotBooks(limit);
        for (PersonalizedRecVO item : fallback) {
            item.setAlgorithmType("POPULAR");
            item.setRecommendTime(now);
        }
        return fallback;
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (Exception ignore) {
            return null;
        }
    }

    private Double toDouble(Object value) {
        if (value == null) {
            return 0D;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (Exception ignore) {
            return 0D;
        }
    }

    private String toStr(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
