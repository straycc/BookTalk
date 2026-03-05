package com.cc.booktalk.application.user.service.recommendation.impl;

import com.cc.booktalk.common.constant.BusinessConstant;
import com.cc.booktalk.common.constant.RedisCacheConstant;
import com.cc.booktalk.common.utils.TimeUtils;
import com.cc.booktalk.domain.entity.book.Book;
import com.cc.booktalk.domain.entity.review.BookReview;
import com.cc.booktalk.domain.entity.recommendation.UserBehaviorLog;
import com.cc.booktalk.domain.entity.recommendation.UserInterestTag;
import com.cc.booktalk.domain.entity.user.UserInfo;
import com.cc.booktalk.domain.recommendation.HotBookRecDomain;
import com.cc.booktalk.domain.recommendation.HotReviewRecDomain;
import com.cc.booktalk.interfaces.vo.user.rec.PersonalizedRecVO;
import com.cc.booktalk.interfaces.vo.user.review.HotReviewVO;
import com.cc.booktalk.infrastructure.persistence.user.mapper.book.BookUserMapper;
import com.cc.booktalk.infrastructure.persistence.user.mapper.recommendation.UserBehaviorLogMapper;
import com.cc.booktalk.infrastructure.persistence.user.mapper.recommendation.UserInfoUserMapper;
import com.cc.booktalk.infrastructure.persistence.user.mapper.recommendation.UserInterestTagMapper;
import com.cc.booktalk.infrastructure.persistence.user.mapper.review.ReviewUserMapper;
import com.cc.booktalk.application.user.service.recommendation.RecommendationService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 个性化推荐服务实现类
 *
 * @author cc
 * @since 2025-10-17
 */
@Slf4j
@Service
public class RecommendationServiceImpl implements RecommendationService {

    @Resource
    private UserInterestTagMapper userInterestTagMapper;

    @Resource
    private UserBehaviorLogMapper userBehaviorLogMapper;

    @Resource
    private BookUserMapper bookUserMapper;

    @Resource
    private ReviewUserMapper reviewUserMapper;

    @Resource
    private UserInfoUserMapper userInfoUserMapper;

    @Resource(name = "customObjectRedisTemplate")
    private RedisTemplate<String, Object> redisTemplate;


    @Resource
    private HotBookRecDomain  hotBookRecDomain;

    @Resource
    private HotReviewRecDomain hotReviewRecDomain;


    /**
     * 缓存过期时间（小时）
     */
    private static final long CACHE_EXPIRE_HOURS = 2;

    /**
     * 默认推荐数量
     */
    private static final int DEFAULT_RECOMMENDATION_COUNT = 10;
    /**
     * 热门推荐缓存预热数量（固定写入，接口再按limit裁剪）
     */
    private static final int HOT_CACHE_PREWARM_COUNT = 50;
    private static final int DEFAULT_HOT_REVIEW_LIMIT = 6;




    /**
     * 获取个性化推荐书籍
     * 基于用户兴趣标签和行为历史推荐
     * @param userId 用户ID
     * @param limit 推荐数量限制
     * @return
     */
    @Override
    public List<PersonalizedRecVO> getPersonalizedRecommendations(Long userId, Integer limit) {
        // 使用默认推荐数量
        int finalLimit = limit != null && limit > 0 ? limit : DEFAULT_RECOMMENDATION_COUNT;

        // 尝试从缓存获取
        String cacheKey = RedisCacheConstant.RECOMMENDATIONS_PREFIX + userId + ":personalized";
        List<PersonalizedRecVO> cachedResults = (List<PersonalizedRecVO>) redisTemplate.opsForValue().get(cacheKey);
        if (cachedResults != null) {
            log.debug("从缓存获取个性化推荐: userId={}, 数量={}", userId, cachedResults.size());
            return cachedResults.stream().limit(finalLimit).collect(Collectors.toList());
        }

        // 混合推荐：结合基于内容和协同过滤，各获取一半数量确保充足
        int halfCount = (finalLimit + 1) / 2; // 向上取整
        List<PersonalizedRecVO> contentBased = getContentBasedRecommendations(userId, halfCount);
        List<PersonalizedRecVO> collaborative = getCollaborativeRecommendations(userId, halfCount);

        // 合并和去重
        Map<Long, PersonalizedRecVO> mergedResults = new LinkedHashMap<>();

        // 添加基于内容的推荐（权重0.6）
        for (PersonalizedRecVO item : contentBased) {
            item.setScore(item.getScore() * 0.6);
            mergedResults.put(item.getBookId(), item);
        }

        // 添加协同过滤推荐（权重0.4）
        for (PersonalizedRecVO item : collaborative) {
            if (mergedResults.containsKey(item.getBookId())) {
                // 如果已存在，合并分数
                PersonalizedRecVO existing = mergedResults.get(item.getBookId());
                existing.setScore(existing.getScore() + item.getScore() * 0.4);
            } else {
                item.setScore(item.getScore() * 0.4);
                mergedResults.put(item.getBookId(), item);
            }
        }

        // 按分数排序并限制数量
        List<PersonalizedRecVO> results = mergedResults.values().stream()
                .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
                .limit(finalLimit)
                .collect(Collectors.toList());

        // 缓存结果
        redisTemplate.opsForValue().set(cacheKey, results, Duration.ofHours(CACHE_EXPIRE_HOURS));
        return results;
    }

    /**
     * 获取基于内容的推荐
     * @param userId 用户ID
     * @param limit 推荐数量限制
     * @return
     */
    @Override
    public List<PersonalizedRecVO> getContentBasedRecommendations(Long userId, Integer limit) {
        try {
            // 获取用户兴趣标签（限制数量，提高性能）
            List<UserInterestTag> userInterests = userInterestTagMapper.getTopUserInterests(userId, 10);
            if (userInterests.isEmpty()) {
                log.debug("用户无兴趣标签，返回热门推荐: userId={}", userId);
                return getHotRecommendations(limit);
            }

            log.debug("获取到用户兴趣标签: userId={}, 标签数量={}", userId, userInterests.size());

            // 获取所有推荐书籍并去重
            Map<Long, PersonalizedRecVO> uniqueBooks = new LinkedHashMap<>();

            // 为每个兴趣标签获取推荐书籍
            int booksPerTag = Math.max(1, limit / userInterests.size());
            for (UserInterestTag interest : userInterests) {
                List<PersonalizedRecVO> books = getBooksByTagName(interest.getTagName(), booksPerTag);

                for (PersonalizedRecVO book : books) {
                    book.setScore(book.getScore() * interest.getInterestScore());
                    uniqueBooks.put(book.getBookId(), book);
                }
            }

            // 按分数排序并限制数量
            List<PersonalizedRecVO> results = uniqueBooks.values().stream()
                    .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
                    .limit(limit)
                    .collect(Collectors.toList());

            log.debug("基于内容的推荐完成: userId={}, 原始书籍数={}, 推荐数量={}",
                     userId, uniqueBooks.size(), results.size());
            return results;

        } catch (Exception e) {
            log.error("获取基于内容的推荐失败: userId={}", userId, e);
            // 降级到热门推荐
            return getHotRecommendations(limit);
        }
    }

    /**
     * 根据标签名获取推荐书籍
     */
    private List<PersonalizedRecVO> getBooksByTagName(String tagName, Integer limit) {
        try {
            if (tagName == null || tagName.trim().isEmpty()) {
                return new ArrayList<>();
            }
            return userInterestTagMapper.getBooksByTagName(tagName.trim(), limit);
        } catch (Exception e) {
            log.error("根据标签名获取推荐书籍失败: tagName={}", tagName, e);
            return new ArrayList<>();
        }
    }

    /**
     * 获取协同过滤推荐
     * @param userId 用户ID
     * @param limit 推荐数量限制
     * @return
     */
    @Override
    public List<PersonalizedRecVO> getCollaborativeRecommendations(Long userId, Integer limit) {
        try {
            log.debug("获取协同过滤推荐: userId={}, limit={}", userId, limit);

            // 找到相似用户
            List<Long> similarUsers = findSimilarUsers(userId, 5);
            if (similarUsers.isEmpty()) {
                log.debug("未找到相似用户: userId={}", userId);
                return new ArrayList<>();
            }

            // 获取相似用户喜欢的书籍
            List<PersonalizedRecVO> recommendations = new ArrayList<>();

            for (Long similarUserId : similarUsers) {
                List<PersonalizedRecVO> userBooks = getUserPreferredBooks(similarUserId, 5);
                recommendations.addAll(userBooks);
            }

            // 去重并按分数排序
            Map<Long, PersonalizedRecVO> uniqueBooks = new LinkedHashMap<>();
            for (PersonalizedRecVO book : recommendations) {
                if (!uniqueBooks.containsKey(book.getBookId())) {
                    uniqueBooks.put(book.getBookId(), book);
                }
            }

            List<PersonalizedRecVO> results = uniqueBooks.values().stream()
                    .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
                    .limit(limit)
                    .collect(Collectors.toList());

            log.debug("协同过滤推荐完成: userId={}, 数量={}", userId, results.size());
            return results;

        } catch (Exception e) {
            log.error("获取协同过滤推荐失败: userId={}", userId, e);
            return new ArrayList<>();
        }
    }


    /**
     * 获取通用热门推荐（用于无用户数据，冷启动时）
     * @param limit 推荐数量限制
     * @return
     */
    @Override
    public List<PersonalizedRecVO> getHotRecommendations(Integer limit) {
        try {
            int finalLimit = limit != null && limit > 0 ? limit : DEFAULT_RECOMMENDATION_COUNT;
            log.debug("获取热门推荐: limit={}", finalLimit);

            // 尝试从缓存获取
            List<PersonalizedRecVO> cachedResults = (List<PersonalizedRecVO>) redisTemplate.opsForValue().get(RedisCacheConstant.RECOMMENDATIONS_HOT);
            if (cachedResults != null && !cachedResults.isEmpty()) {
                log.debug("从缓存获取热门推荐: 数量={}", cachedResults.size());
                return cachedResults.stream().limit(finalLimit).collect(Collectors.toList());
            }

            // 缓存未命中时固定预热较大集合，避免被某次小limit请求“缩容”
            List<PersonalizedRecVO> refreshed = refreshHotRecommendationsCache(HOT_CACHE_PREWARM_COUNT);
            return refreshed.stream().limit(finalLimit).collect(Collectors.toList());

        } catch (Exception e) {
            log.error("获取热门推荐失败", e);
            return new ArrayList<>();
        }
    }

    /**
     * 刷新书籍热门缓存
     * @param limit 缓存的推荐数量
     * @return
     */
    @Override
    public List<PersonalizedRecVO> refreshHotRecommendationsCache(Integer limit) {
        try {
            int finalLimit = limit != null && limit > 0 ? limit : HOT_CACHE_PREWARM_COUNT;
            List<PersonalizedRecVO> hotBooks = calculateHotRecommendations(finalLimit);
            cacheHotRecommendationsIfNotEmpty(hotBooks);
            log.info("热门推荐缓存刷新完成: 数量={}", hotBooks.size());
            return hotBooks;
        } catch (Exception e) {
            log.error("刷新热门推荐缓存失败", e);
            return new ArrayList<>();
        }
    }

    /**
     * 获取热门书评
     * @param period 时间周期: daily/weekly/monthly 或 24h/7d/30d
     * @param limit 推荐数量
     * @return
     */
    @Override
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

    /**
     * 刷新热门书评
     * @param period 时间周期: daily/weekly/monthly 或 24h/7d/30d
     * @param limit 缓存数量
     * @return
     */
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


    /**
     * 计算书籍热度值
     * @param finalLimit
     * @return
     */
    private List<PersonalizedRecVO> calculateHotRecommendations(int finalLimit) {
        // 1.先按行为活跃度拿候选集合（扩大候选池，便于后续按衰减重排）
        int candidateLimit = Math.max(finalLimit * 5, 50);
        List<Long> candidateBookIds = userBehaviorLogMapper.getHotBookCandidateIds(30, candidateLimit);
        if (candidateBookIds == null || candidateBookIds.isEmpty()) {
            return buildFallbackHotBooks(finalLimit, now());
        }

        // 2. 批量拿图书基础信息
        List<PersonalizedRecVO> candidateBooks = bookUserMapper.getRecBookBaseByIds(candidateBookIds);
        Map<Long, PersonalizedRecVO> bookInfoMap = candidateBooks.stream()
                .collect(Collectors.toMap(PersonalizedRecVO::getBookId, it -> it, (a, b) -> a, LinkedHashMap::new));

        // 3. 使用Domain规则计算带时间衰减的热度分数
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

        rankedBooks.sort((a, b) -> Double.compare(
                b.getScore() == null ? 0.0 : b.getScore(),
                a.getScore() == null ? 0.0 : a.getScore()
        ));
        List<PersonalizedRecVO> hotBooks = rankedBooks.stream()
                .limit(finalLimit)
                .collect(Collectors.toList());

        // 行为热榜不足时，使用基础热门补齐到limit
        if (hotBooks.size() < finalLimit) {
            int need = finalLimit - hotBooks.size();
            List<PersonalizedRecVO> fallback = buildFallbackHotBooks(finalLimit, now());
            Set<Long> existed = hotBooks.stream()
                    .map(PersonalizedRecVO::getBookId)
                    .collect(Collectors.toSet());
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


    //当前时间
    private LocalDateTime now() {
        return LocalDateTime.now();
    }

    //热门书籍推荐兜底策略
    private List<PersonalizedRecVO> buildFallbackHotBooks(int limit, LocalDateTime now) {
        List<PersonalizedRecVO> fallback = bookUserMapper.getFallbackHotBooks(limit);
        if (fallback == null || fallback.isEmpty()) {
            return new ArrayList<>();
        }
        for (PersonalizedRecVO item : fallback) {
            item.setAlgorithmType("POPULAR");
            item.setRecommendTime(now);
        }
        return fallback;
    }

    private void cacheHotRecommendationsIfNotEmpty(List<PersonalizedRecVO> hotBooks) {
        if (hotBooks == null || hotBooks.isEmpty()) {
            return;
        }
        // 与定时任务周期（6小时）保持一致，减少缓存空窗期
        redisTemplate.opsForValue().set(RedisCacheConstant.RECOMMENDATIONS_HOT, hotBooks, Duration.ofHours(6));
    }

    private List<HotReviewVO> calculateHotReviewRecommendations(String period, int limit) {
        int days = resolveReviewDays(period);
        int candidateLimit = Math.max(limit * 5, 50);
        List<Long> candidateReviewIds = userBehaviorLogMapper.getHotReviewCandidateIds(days, candidateLimit);
        if (candidateReviewIds == null || candidateReviewIds.isEmpty()) {
            return getLatestReviewFallback(limit);
        }

        Map<Long, BookReview> reviewMap = reviewUserMapper.selectBatchIds(candidateReviewIds).stream()
                .collect(Collectors.toMap(BookReview::getId, it -> it, (a, b) -> a));

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

        rankedReviews.sort((a, b) -> Double.compare(
                b.getHotScore(),
                a.getHotScore()
        ));
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
            case "7d":
            case "week":
            case "weekly":
                return "weekly";
            case "30d":
            case "month":
            case "monthly":
                return "monthly";
            default:
                return "weekly";
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

    /**
     * 清楚用户推荐缓存
     * @param userId 用户ID
     */
    @Override
    public void clearRecommendationCache(Long userId) {
        String pattern = RedisCacheConstant.RECOMMENDATIONS_PREFIX + userId + ":*";
        Set<String> keys = redisTemplate.keys(pattern);
        if (!keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.debug("清除用户推荐缓存: userId={}, 清除{}个缓存键", userId, keys.size());
        }
    }


    /**
     * 找到相似用户
     * 基于用户Top5兴趣标签计算相似度
     */
    private List<Long> findSimilarUsers(Long userId, Integer limit) {
        try {
            // 先从Redis获取当前用户的Top5兴趣标签
            List<UserInterestTag> currentUserInterests = getCachedUserTopInterests(userId);

            // 如果Redis缓存不存在，从数据库获取
            if (currentUserInterests == null || currentUserInterests.isEmpty()) {
                currentUserInterests = userInterestTagMapper.getTopUserInterests(userId, 5);
            }

            if (currentUserInterests.isEmpty()) {
                return new ArrayList<>();
            }

            // 基于Top5标签的标签重叠度计算相似用户
            return findSimilarUsersByTagOverlap(currentUserInterests, limit);

        } catch (Exception e) {
            log.error("查找相似用户失败: userId={}", userId, e);
            return new ArrayList<>();
        }
    }

    /**
     * 从Redis获取用户缓存的Top5兴趣标签
     */
    private List<UserInterestTag> getCachedUserTopInterests(Long userId) {
        try {
            String cacheKey = RedisCacheConstant.USER_TOP_INTEREST_PREFIX + userId;
            return (List<UserInterestTag>) redisTemplate.opsForValue().get(cacheKey);
        } catch (Exception e) {
            log.error("获取用户兴趣缓存失败: userId={}", userId, e);
            return null;
        }
    }

    /**
     * 基于标签重叠度计算相似用户
     */
    private List<Long> findSimilarUsersByTagOverlap(List<UserInterestTag> currentUserInterests, Integer limit) {
        // 获取当前用户的标签集合
        Set<String> currentUserTags = currentUserInterests.stream()
                .map(UserInterestTag::getTagName)
                .collect(Collectors.toSet());

        // 查询有共同标签的其他用户
        List<Long> similarUsers = new ArrayList<>();

        for (String tag : currentUserTags) {
            List<Long> usersWithSameTag = userInterestTagMapper.getUsersByTag(tag, 20);
            similarUsers.addAll(usersWithSameTag);
        }

        // 统计每个用户的标签重叠度
        Map<Long, Integer> tagOverlapCount = new HashMap<>();
        for (Long otherUserId : similarUsers) {
            if (otherUserId.equals(currentUserInterests.get(0).getUserId())) {
                continue; // 跳过自己
            }

            // 获取其他用户的Top5标签
            List<UserInterestTag> otherUserInterests = getCachedUserTopInterests(otherUserId);
            if (otherUserInterests == null || otherUserInterests.isEmpty()) {
                otherUserInterests = userInterestTagMapper.getTopUserInterests(otherUserId, 5);
            }

            // 计算标签重叠数量
            Set<String> otherUserTags = otherUserInterests.stream()
                    .map(UserInterestTag::getTagName)
                    .collect(Collectors.toSet());

            int overlapCount = 0;
            for (String tag : currentUserTags) {
                if (otherUserTags.contains(tag)) {
                    overlapCount++;
                }
            }

            if (overlapCount > 0) {
                tagOverlapCount.put(otherUserId, overlapCount);
            }
        }

        // 按重叠度排序并返回前N个用户
        return tagOverlapCount.entrySet().stream()
                .sorted(Map.Entry.<Long, Integer>comparingByValue().reversed())
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /**
     * 获取用户偏好的书籍（基于用户行为日志）
     */
    private List<PersonalizedRecVO> getUserPreferredBooks(Long userId, Integer limit) {
        List<PersonalizedRecVO> preferredBooks = userBehaviorLogMapper.getHighScoreBooks(userId, limit);
        if (preferredBooks.isEmpty()) return new ArrayList<>();

        return preferredBooks.stream()
                .peek(book -> {
                    book.setReason(BusinessConstant.RECOMMENDATION_REASON);
                    book.setScore(book.getScore() * 0.8);
                })
                .collect(Collectors.toList());
    }


}
