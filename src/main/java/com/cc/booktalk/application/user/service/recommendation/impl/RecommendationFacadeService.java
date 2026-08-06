package com.cc.booktalk.application.user.service.recommendation.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cc.booktalk.application.user.service.recommendation.HotRecommendationService;
import com.cc.booktalk.application.user.service.recommendation.RecommendationService;
import com.cc.booktalk.application.user.service.recommendation.profile.UserProfileService;
import com.cc.booktalk.common.constant.RedisCacheConstant;
import com.cc.booktalk.common.utils.TimeUtils;
import com.cc.booktalk.domain.entity.book.Book;
import com.cc.booktalk.domain.entity.bookShelf.BookShelf;
import com.cc.booktalk.domain.entity.review.BookReview;
import com.cc.booktalk.domain.entity.recommendation.UserBehaviorLog;
import com.cc.booktalk.domain.entity.user.UserInfo;
import com.cc.booktalk.domain.recommendation.HotReviewRecDomain;
import com.cc.booktalk.domain.recommendation.model.RecommendationBookCandidate;
import com.cc.booktalk.domain.recommendation.model.UserProfileSnapshot;
import com.cc.booktalk.infrastructure.persistence.user.mapper.book.BookUserMapper;
import com.cc.booktalk.infrastructure.persistence.user.mapper.bookShelf.BookShelfMapper;
import com.cc.booktalk.infrastructure.persistence.user.mapper.recommendation.UserBehaviorLogMapper;
import com.cc.booktalk.infrastructure.persistence.user.mapper.recommendation.UserInterestTagMapper;
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
    private HotRecommendationService hotRecommendationService;

    @Resource
    private ReviewUserMapper reviewUserMapper;

    @Resource
    private UserInfoUserMapper userInfoUserMapper;

    @Resource
    private BookUserMapper bookUserMapper;

    @Resource
    private BookShelfMapper bookShelfMapper;

    @Resource
    private UserBehaviorLogMapper userBehaviorLogMapper;

    @Resource
    private UserInterestTagMapper userInterestTagMapper;

    @Resource
    private HotReviewRecDomain hotReviewRecDomain;

    @Resource(name = "customObjectRedisTemplate")
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    @SuppressWarnings("unchecked")
    public List<PersonalizedRecVO> getPersonalizedRecommendations(Long userId, Integer limit) {
        int finalLimit = normalizeLimit(limit);
        if (userId == null || userId <= 0) {
            return getFilteredHotRecommendations(null, finalLimit);
        }
        String cacheKey = buildRecommendationCacheKey(userId);
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached instanceof List && !((List<?>) cached).isEmpty()) {
                List<?> cachedResults = (List<?>) cached;
                if (cachedResults.stream().allMatch(PersonalizedRecVO.class::isInstance)) {
                    List<PersonalizedRecVO> visibleCachedResults = filterCachedRecommendations(userId, cachedResults.stream()
                            .map(PersonalizedRecVO.class::cast)
                            .collect(Collectors.toList()));
                    if (visibleCachedResults.size() >= finalLimit) {
                        return visibleCachedResults.stream().limit(finalLimit).collect(Collectors.toList());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("读取推荐缓存失败，改用在线计算: userId={}", userId, e);
        }

        List<PersonalizedRecVO> recommendations;
        try {
            UserProfileSnapshot snapshot = userProfileService.buildSnapshot(userId);
            if (snapshot.isEmpty()) {
                recommendations = getFilteredHotRecommendations(userId, finalLimit);
            } else {
                List<RecommendationBookCandidate> candidates = filterCandidates(userId,
                        recallCandidates(snapshot, finalLimit));
                List<RecommendationBookCandidate> rankedCandidates = rankCandidates(candidates, finalLimit);
                rankedCandidates.forEach(candidate -> candidate.setRecommendTime(LocalDateTime.now()));
                recommendations = buildRecommendations(rankedCandidates);
                if (recommendations.isEmpty()) {
                    recommendations = getFilteredHotRecommendations(userId, finalLimit);
                }
            }
        } catch (Exception e) {
            log.warn("个性化推荐计算失败，改用热门兜底: userId={}", userId, e);
            recommendations = getFilteredHotRecommendations(userId, finalLimit);
        }

        if (!recommendations.isEmpty()) {
            try {
                redisTemplate.opsForValue().set(cacheKey, recommendations, Duration.ofHours(6));
            } catch (Exception e) {
                log.warn("写入推荐缓存失败: userId={}", userId, e);
            }
        }
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

        try {
            List<HotReviewVO> cached = (List<HotReviewVO>) redisTemplate.opsForValue().get(cacheKey);
            if (cached != null && !cached.isEmpty()) {
                return cached.stream().limit(finalLimit).collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.warn("读取热门书评推荐缓存失败，改用数据库计算: period={}", normalizedPeriod, e);
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
            try {
                redisTemplate.opsForValue().set(cacheKey, hotReviews, resolveReviewCacheTtl(normalizedPeriod));
            } catch (Exception e) {
                log.warn("写入热门书评推荐缓存失败: period={}", normalizedPeriod, e);
            }
        }
        return hotReviews;
    }

    @Override
    public void clearRecommendationCache(Long userId) {
        if (userId == null || userId <= 0) {
            return;
        }
        String pattern = RedisCacheConstant.RECOMMENDATIONS_PREFIX + userId + ":*";
        try {
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception e) {
            log.warn("清理用户推荐缓存失败: userId={}", userId, e);
        }
    }

    private String buildRecommendationCacheKey(Long userId) {
        return RedisCacheConstant.RECOMMENDATIONS_PREFIX + userId + ":books";
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_RECOMMENDATION_COUNT;
        }
        return Math.min(limit, 50);
    }

    private List<PersonalizedRecVO> getFilteredHotRecommendations(Long userId, Integer limit) {
        int finalLimit = limit != null && limit > 0 ? limit : DEFAULT_RECOMMENDATION_COUNT;
        List<PersonalizedRecVO> hotRecommendations = hotRecommendationService.getHotRecommendations(finalLimit * 3);
        if (hotRecommendations == null || hotRecommendations.isEmpty()) {
            hotRecommendations = bookUserMapper.getFallbackHotBooks(finalLimit * 3);
        }
        if (hotRecommendations == null || hotRecommendations.isEmpty()) {
            return List.of();
        }

        List<RecommendationBookCandidate> hotCandidates = hotRecommendations.stream()
                .map(item -> RecommendationBookCandidate.builder()
                        .bookId(item.getBookId())
                        .bookTitle(item.getBookTitle())
                        .author(item.getAuthor())
                        .bookCover(item.getBookCover())
                        .hotScore(item.getScore())
                        .score(item.getScore())
                        .recommendTime(item.getRecommendTime())
                        .build())
                .collect(Collectors.toList());
        List<RecommendationBookCandidate> filtered = filterCandidates(userId, hotCandidates);
        // 当用户已经与所有热门候选发生过交互时，仍返回热门结果，避免推荐区空白。
        List<RecommendationBookCandidate> resultCandidates = filtered.isEmpty() ? hotCandidates : filtered;
        return resultCandidates.stream()
                .limit(finalLimit)
                .map(item -> PersonalizedRecVO.builder()
                        .bookId(item.getBookId())
                        .bookTitle(item.getBookTitle())
                        .author(item.getAuthor())
                        .bookCover(item.getBookCover())
                        .score(item.getScore())
                        .reason("热门趋势推荐")
                        .reasonCodes(List.of("HOT_FALLBACK"))
                        .sourceStrategies(List.of("HOT_FALLBACK"))
                        .confidence(0.45)
                        .algorithmType("POPULAR")
                        .recommendTime(item.getRecommendTime() == null ? LocalDateTime.now() : item.getRecommendTime())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 推荐流水线当前收敛在门面服务内部，避免在当前阶段拆成多个过薄的 service。
     */
    private List<RecommendationBookCandidate> recallCandidates(UserProfileSnapshot snapshot, Integer limit) {
        int finalLimit = limit == null || limit <= 0 ? DEFAULT_RECOMMENDATION_COUNT : limit;
        int perRouteLimit = Math.max(finalLimit * 2, 10);
        Map<Long, RecommendationBookCandidate> merged = new LinkedHashMap<>();

        for (String tag : snapshot.getTopTags()) {
            mergeByTag(merged, tag, perRouteLimit);
        }
        if (!snapshot.getTopCategoryIds().isEmpty()) {
            mergeByCategories(merged, snapshot.getTopCategoryIds(), perRouteLimit);
        }
        if (!snapshot.getTopAuthors().isEmpty()) {
            mergeByAuthors(merged, snapshot.getTopAuthors(), perRouteLimit);
        }
        mergeHotFallback(merged, perRouteLimit);

        return new ArrayList<>(merged.values());
    }

    private void mergeByTag(Map<Long, RecommendationBookCandidate> merged, String tag, int limit) {
        List<RecommendationBookCandidate> books = userInterestTagMapper.getBooksByTagName(tag, limit);
        for (RecommendationBookCandidate candidate : safeList(books)) {
            if (candidate == null || candidate.getBookId() == null) {
                continue;
            }
            RecommendationBookCandidate target = merged.computeIfAbsent(candidate.getBookId(), key -> candidate);
            target.getMatchedTags().add(tag);
            target.getSourceStrategies().add("TAG_RECALL");
        }
    }

    private void mergeByCategories(Map<Long, RecommendationBookCandidate> merged, List<Long> categoryIds, int limit) {
        List<RecommendationBookCandidate> books = bookUserMapper.findBooksByCategoryIds(categoryIds, limit);
        for (RecommendationBookCandidate candidate : safeList(books)) {
            if (candidate == null || candidate.getBookId() == null) {
                continue;
            }
            RecommendationBookCandidate target = merged.computeIfAbsent(candidate.getBookId(), key -> candidate);
            if (candidate.getCategoryName() != null) {
                target.getMatchedCategories().add(candidate.getCategoryName());
            }
            target.getSourceStrategies().add("CATEGORY_RECALL");
        }
    }

    private void mergeByAuthors(Map<Long, RecommendationBookCandidate> merged, List<String> authors, int limit) {
        List<RecommendationBookCandidate> books = bookUserMapper.findBooksByAuthors(authors, limit);
        for (RecommendationBookCandidate candidate : safeList(books)) {
            if (candidate == null || candidate.getBookId() == null) {
                continue;
            }
            RecommendationBookCandidate target = merged.computeIfAbsent(candidate.getBookId(), key -> candidate);
            if (candidate.getAuthor() != null) {
                target.getMatchedAuthors().add(candidate.getAuthor());
            }
            target.getSourceStrategies().add("AUTHOR_RECALL");
        }
    }

    private void mergeHotFallback(Map<Long, RecommendationBookCandidate> merged, int limit) {
        List<PersonalizedRecVO> hotBooks = hotRecommendationService.getHotRecommendations(limit);
        for (PersonalizedRecVO hotBook : safeList(hotBooks)) {
            if (hotBook == null || hotBook.getBookId() == null) {
                continue;
            }
            RecommendationBookCandidate target = merged.computeIfAbsent(hotBook.getBookId(), key -> RecommendationBookCandidate.builder()
                    .bookId(hotBook.getBookId())
                    .bookTitle(hotBook.getBookTitle())
                    .author(hotBook.getAuthor())
                    .bookCover(hotBook.getBookCover())
                    .hotScore(hotBook.getScore())
                    .build());
            target.getSourceStrategies().add("HOT_FALLBACK");
        }
    }

    private List<RecommendationBookCandidate> filterCandidates(Long userId, List<RecommendationBookCandidate> candidates) {
        if (userId == null || candidates == null || candidates.isEmpty()) {
            return candidates == null ? List.of() : candidates;
        }

        Set<Long> excludedBookIds = loadExcludedBookIds(userId);
        List<RecommendationBookCandidate> filtered = new ArrayList<>();
        for (RecommendationBookCandidate candidate : candidates) {
            if (candidate.getBookId() == null || excludedBookIds.contains(candidate.getBookId())) {
                continue;
            }
            filtered.add(candidate);
        }
        return filtered;
    }

    private List<PersonalizedRecVO> filterCachedRecommendations(Long userId, List<PersonalizedRecVO> recommendations) {
        if (recommendations == null || recommendations.isEmpty()) {
            return List.of();
        }
        Set<Long> excludedBookIds = loadExcludedBookIds(userId);
        Set<Long> seenBookIds = new HashSet<>();
        return recommendations.stream()
                .filter(Objects::nonNull)
                .filter(item -> item.getBookId() != null)
                .filter(item -> !excludedBookIds.contains(item.getBookId()))
                .filter(item -> seenBookIds.add(item.getBookId()))
                .collect(Collectors.toList());
    }

    private Set<Long> loadExcludedBookIds(Long userId) {
        Set<Long> excluded = new HashSet<>();

        List<BookShelf> shelfItems = bookShelfMapper.selectList(new LambdaQueryWrapper<BookShelf>()
                .eq(BookShelf::getUserId, userId));
        excluded.addAll(safeList(shelfItems).stream()
                .filter(Objects::nonNull)
                .map(BookShelf::getBookId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));

        List<BookReview> reviews = reviewUserMapper.selectList(new LambdaQueryWrapper<BookReview>()
                .eq(BookReview::getUserId, userId));
        excluded.addAll(safeList(reviews).stream()
                .filter(Objects::nonNull)
                .map(BookReview::getBookId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));

        List<Long> strongInteracted = userBehaviorLogMapper.getStrongInteractedBookIds(userId, 180);
        if (strongInteracted != null && !strongInteracted.isEmpty()) {
            excluded.addAll(strongInteracted);
        }

        return excluded;
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }

    private List<RecommendationBookCandidate> rankCandidates(List<RecommendationBookCandidate> candidates, Integer limit) {
        int finalLimit = limit == null || limit <= 0 ? DEFAULT_RECOMMENDATION_COUNT : limit;
        return candidates.stream()
                .peek(this::scoreCandidate)
                .sorted(Comparator.comparing(RecommendationBookCandidate::getScore, Comparator.nullsLast(Double::compareTo)).reversed())
                .limit(finalLimit)
                .collect(Collectors.toList());
    }

    private void scoreCandidate(RecommendationBookCandidate candidate) {
        double tagScore = candidate.getMatchedTags().size() * 30D;
        double categoryScore = candidate.getMatchedCategories().size() * 20D;
        double authorScore = candidate.getMatchedAuthors().size() * 25D;
        double qualityScore = safe(candidate.getAverageScore()) * 4
                + logScore(candidate.getScoreCount(), 5)
                + logScore(candidate.getFavoriteCount(), 6);
        double hotScore = safe(candidate.getHotScore()) * 0.2;
        double freshnessScore = calculateFreshnessScore(candidate.getCreateTime());
        double totalScore = tagScore + categoryScore + authorScore + qualityScore + hotScore + freshnessScore;

        double confidence = Math.min(1D, 0.35
                + candidate.getSourceStrategies().size() * 0.15
                + candidate.getMatchedTags().size() * 0.1
                + candidate.getMatchedCategories().size() * 0.08
                + candidate.getMatchedAuthors().size() * 0.1);

        candidate.setScore(totalScore);
        candidate.setConfidence(confidence);
    }

    private double calculateFreshnessScore(LocalDateTime createTime) {
        if (createTime == null) {
            return 0D;
        }
        long days = Math.max(0, Duration.between(createTime, LocalDateTime.now()).toDays());
        if (days <= 30) {
            return 8D;
        }
        if (days <= 90) {
            return 4D;
        }
        return 0D;
    }

    private double logScore(Integer value, double multiplier) {
        int safeValue = value == null ? 0 : value;
        return Math.log10(safeValue + 1D) * multiplier;
    }

    private double safe(Double value) {
        return value == null ? 0D : value;
    }

    private List<PersonalizedRecVO> buildRecommendations(List<RecommendationBookCandidate> candidates) {
        List<PersonalizedRecVO> results = new ArrayList<>();
        for (RecommendationBookCandidate candidate : candidates) {
            List<String> reasonCodes = new ArrayList<>(candidate.getReasonCodes());
            if (!candidate.getMatchedTags().isEmpty()) {
                reasonCodes.add("TAG_MATCH");
            }
            if (!candidate.getMatchedCategories().isEmpty()) {
                reasonCodes.add("CATEGORY_MATCH");
            }
            if (!candidate.getMatchedAuthors().isEmpty()) {
                reasonCodes.add("AUTHOR_MATCH");
            }
            if (reasonCodes.isEmpty()) {
                reasonCodes.add("HOT_FALLBACK");
            }

            PersonalizedRecVO recommendation = PersonalizedRecVO.builder()
                    .bookId(candidate.getBookId())
                    .bookTitle(candidate.getBookTitle())
                    .author(candidate.getAuthor())
                    .bookCover(candidate.getBookCover())
                    .score(candidate.getScore())
                    .reason(buildReason(candidate))
                    .reasonCodes(reasonCodes)
                    .matchedTags(new ArrayList<>(candidate.getMatchedTags()))
                    .matchedCategories(new ArrayList<>(candidate.getMatchedCategories()))
                    .matchedAuthors(new ArrayList<>(candidate.getMatchedAuthors()))
                    .sourceStrategies(new ArrayList<>(candidate.getSourceStrategies()))
                    .confidence(candidate.getConfidence())
                    .recommendTime(candidate.getRecommendTime() == null ? LocalDateTime.now() : candidate.getRecommendTime())
                    .algorithmType("HYBRID")
                    .relatedInterests(buildRelatedInterests(candidate))
                    .build();
            results.add(recommendation);
        }
        return results;
    }

    private String buildReason(RecommendationBookCandidate candidate) {
        if (!candidate.getMatchedTags().isEmpty() && !candidate.getMatchedCategories().isEmpty()) {
            return String.format("命中您关注的标签“%s”和分类“%s”",
                    candidate.getMatchedTags().iterator().next(),
                    candidate.getMatchedCategories().iterator().next());
        }
        if (!candidate.getMatchedTags().isEmpty()) {
            return String.format("命中您关注的标签“%s”", candidate.getMatchedTags().iterator().next());
        }
        if (!candidate.getMatchedCategories().isEmpty()) {
            return String.format("命中您关注的分类“%s”", candidate.getMatchedCategories().iterator().next());
        }
        if (!candidate.getMatchedAuthors().isEmpty()) {
            return String.format("命中您偏好的作者“%s”", candidate.getMatchedAuthors().iterator().next());
        }
        return "结合热门趋势与图书质量为您推荐";
    }

    private String buildRelatedInterests(RecommendationBookCandidate candidate) {
        List<String> parts = new ArrayList<>();
        parts.addAll(candidate.getMatchedTags());
        parts.addAll(candidate.getMatchedCategories());
        parts.addAll(candidate.getMatchedAuthors());
        return String.join(" / ", parts);
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
