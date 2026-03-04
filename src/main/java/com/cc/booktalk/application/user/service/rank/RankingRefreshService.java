package com.cc.booktalk.application.user.service.rank;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.cc.booktalk.common.constant.RedisCacheConstant;
import com.cc.booktalk.common.utils.TimeUtils;
import com.cc.booktalk.entity.entity.book.Book;
import com.cc.booktalk.entity.entity.recommendation.UserBehaviorLog;
import com.cc.booktalk.entity.entity.review.BookReview;
import com.cc.booktalk.entity.entity.user.UserInfo;
import com.cc.booktalk.entity.vo.BookRankingVO;
import com.cc.booktalk.entity.vo.HotReviewVO;
import com.cc.booktalk.domain.rank.RankingDomainService;
import com.cc.booktalk.infrastructure.persistence.user.mapper.book.BookUserMapper;
import com.cc.booktalk.infrastructure.persistence.user.mapper.recommendation.UserBehaviorLogMapper;
import com.cc.booktalk.infrastructure.persistence.user.mapper.recommendation.UserInfoUserMapper;
import com.cc.booktalk.infrastructure.persistence.user.mapper.review.ReviewUserMapper;

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
import java.util.stream.Collectors;

@Service
@Slf4j
public class RankingRefreshService {

    @Resource
    private BookUserMapper bookUserMapper;

    @Resource
    private ReviewUserMapper reviewUserMapper;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private UserInfoUserMapper userInfoUserMapper;

    @Resource
    private UserBehaviorLogMapper userBehaviorLogMapper;

    @Resource
    private RankingDomainService rankingDomainService;

    /**
     * 每1小时计算一次书籍热度分数
     * 基于用户行为日志计算每本书的热度值，并更新到book表的hot_score字段
     */
    public void calculateBookHotScores() {
        try {
            log.info("开始计算书籍热度分数");
            // 获取最近7天有用户行为的书籍ID
            LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
            List<Long> activeBookIds = getActiveBookIds(sevenDaysAgo);

            if (activeBookIds.isEmpty()) {
                log.info("最近7天无活跃书籍，跳过热度计算");
                return;
            }

            int updatedCount = 0;
            // 分批处理，每批100本书
            int batchSize = 100;
            for (int i = 0; i < activeBookIds.size(); i += batchSize) {
                int endIndex = Math.min(i + batchSize, activeBookIds.size());
                List<Long> batchBooks = activeBookIds.subList(i, endIndex);

                for (Long bookId : batchBooks) {
                    try {
                        Double hotScore = calculateBookHotScore(bookId, sevenDaysAgo);
                        updateBookHotScore(bookId, hotScore);
                        updatedCount++;
                    } catch (Exception e) {
                        log.error("计算书籍热度失败: bookId={}", bookId, e);
                    }
                }
            }
            log.info("书籍热度计算完成: 更新数量={}", updatedCount);
        } catch (Exception e) {
            log.error("计算书籍热度分数失败", e);
        }
    }

    /**
     * 每30分钟计算一次书评热度分数
     * 基于用户行为日志计算每条书评的热度值
     */

    @Transactional
    public void calculateReviewHotScores() {
        try {
            log.info("开始计算书评热度分数");

            // 获取最近7天的书评
            LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
            LambdaQueryWrapper<BookReview> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.ge(BookReview::getCreateTime, sevenDaysAgo);

            List<BookReview> reviews = reviewUserMapper.selectList(queryWrapper);
            if (reviews.isEmpty()) {
                log.info("最近7天无书评，跳过热度计算");
                return;
            }

            int updatedCount = 0;
            for (BookReview review : reviews) {
                try {
                    Double hotScore = calculateReviewHotScore(review.getId(), sevenDaysAgo);
                    updateReviewHotScore(review.getId(), hotScore);
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

    /**
     * 每2小时更新一次本周热门书籍榜单到Redis
     */

    public void updateWeeklyHotBooksToRedis() {
        try {
            log.info("开始更新本周热门书籍榜单到Redis");
            // 只计算本周热门（最近7天）
            List<BookRankingVO> weeklyRankings = getBookRankingsByPeriod("weekly", 50);
            // 存储到Redis
            String redisKey = RedisCacheConstant.RANKING_HOT_BOOKS_PREFIX + "weekly";
            redisTemplate.opsForValue().set(redisKey, weeklyRankings, Duration.ofHours(2));
            log.info("本周热门书籍榜单更新完成: count={}", weeklyRankings.size());
        } catch (Exception e) {
            log.error("更新本周热门书籍榜单失败", e);
        }
    }

    /**
     * 每30分钟更新一次本周热门书评榜单到Redis
     */

    public void updateWeeklyHotReviewsToRedis() {
        try {
            log.info("开始更新本周热门书评榜单到Redis");
            // 只计算本周热门
            List<HotReviewVO> weeklyHotReviews = getHotReviewsByPeriod("weekly", 50);
            // 存储到Redis
            String redisKey = RedisCacheConstant.RANKING_HOT_REVIEWS_PREFIX + "weekly";
            redisTemplate.opsForValue().set(redisKey, weeklyHotReviews, Duration.ofMinutes(30));
            log.info("本周热门书评榜单更新完成: count={}", weeklyHotReviews.size());
        } catch (Exception e) {
            log.error("更新本周热门书评榜单失败", e);
        }
    }

    /**
     * 根据时间周期获取书籍榜单
     *
     * @param period 时间周期
     * @param limit  限制数量
     * @return 书籍榜单列表
     */
    private List<BookRankingVO> getBookRankingsByPeriod(String period, Integer limit) {
        try {
            // 构建查询条件
            LambdaQueryWrapper<Book> queryWrapper = new LambdaQueryWrapper<>();

            // 根据时间周期设置查询条件
            LocalDateTime startTime = TimeUtils.getStartTimeByPeriod(period);
            queryWrapper.ge(Book::getCreateTime, startTime)
                    .orderByDesc(Book::getHotScore)
                    .orderByDesc(Book::getCreateTime)
                    .last("LIMIT " + limit);

            // 查询书籍数据
            List<Book> books = bookUserMapper.selectList(queryWrapper);

            // 转换为BookRankingVO
            return books.stream()
                    .map(this::convertBookToRankingVO)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("获取书籍榜单失败: period={}", period, e);
            return List.of();
        }
    }

    /**
     * 根据时间周期获取热门书评
     *
     * @param period 时间周期
     * @param limit  限制数量
     * @return 热门书评列表
     */
    private List<HotReviewVO> getHotReviewsByPeriod(String period, Integer limit) {
        try {
            // 构建查询条件
            LambdaQueryWrapper<BookReview> queryWrapper = new LambdaQueryWrapper<>();

            // 根据时间周期设置查询条件
            LocalDateTime startTime = TimeUtils.getStartTimeByPeriod(period);
            queryWrapper.ge(BookReview::getCreateTime, startTime)
                    .orderByDesc(BookReview::getHotScore)
                    .orderByDesc(BookReview::getCreateTime)
                    .last("LIMIT " + limit);

            // 查询书评数据
            List<BookReview> reviews = reviewUserMapper.selectList(queryWrapper);

            // 转换为HotReviewVO
            return reviews.stream()
                    .map(this::convertToHotReviewVO)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("获取热门书评失败: period={}", period, e);
            return List.of();
        }
    }

    /**
     * 将Book转换为BookRankingVO
     */
    private BookRankingVO convertBookToRankingVO(Book book) {
        BookRankingVO ranking = new BookRankingVO();
        BeanUtils.copyProperties(book, ranking);
        ranking.setBookId(book.getId());
        ranking.setBookTitle(book.getTitle());
        ranking.setBookCover(book.getCoverUrl());
        ranking.setAvgRating(book.getAverageScore());
        ranking.setRatingCount(book.getScoreCount());
        ranking.setReadCount(book.getFavoriteCount() != null ? book.getFavoriteCount().longValue() : 0L);
        ranking.setPublishTime(book.getCreateTime());
        ranking.setHotScore(book.getHotScore());
        ranking.setCategoryId(book.getCategoryId());

        return ranking;
    }

    /**
     * 将BookReview转换为HotReviewVO
     */
    private HotReviewVO convertToHotReviewVO(BookReview review) {
        HotReviewVO hotReview = new HotReviewVO();
        BeanUtils.copyProperties(review, hotReview);
        hotReview.setReviewId(review.getId());
        hotReview.setBookId(review.getBookId());
        hotReview.setAuthor("user");//TODO
        hotReview.setContent(review.getContent());
        hotReview.setHotScore(review.getHotScore());
        hotReview.setTimeDesc(TimeUtils.getTimeDesc(review.getCreateTime()));


        // 这里需要注入相关的Mapper，暂时设置默认值
        UserInfo userInfo = userInfoUserMapper.selectById(review.getUserId());
        hotReview.setAuthor(userInfo.getNickname());
        hotReview.setAuthorAvatar(userInfo.getAvatarUrl());

        Book book = bookUserMapper.selectById(review.getBookId());
        if (book != null) {
            hotReview.setBookId(book.getId());
            hotReview.setBookName(book.getTitle());
            hotReview.setBookCover(book.getCoverUrl());
            hotReview.setCategoryId(book.getCategoryId());
        }

        return hotReview;
    }


    // ==================== 热度计算相关方法 ====================

    /**
     * 获取最近有用户行为的书籍ID列表
     * 包含直接书籍行为和书评相关行为
     */
    private List<Long> getActiveBookIds(LocalDateTime since) {
        List<Long> activeBookIds = new ArrayList<>();

        // 1. 获取直接书籍行为的书籍ID
        LambdaQueryWrapper<UserBehaviorLog> bookQueryWrapper = new LambdaQueryWrapper<>();
        bookQueryWrapper.ge(UserBehaviorLog::getCreateTime, since)
                .eq(UserBehaviorLog::getTargetType, "BOOK")
                .select(UserBehaviorLog::getTargetId);

        List<Long> directBookIds = userBehaviorLogMapper.selectList(bookQueryWrapper).stream()
                .map(UserBehaviorLog::getTargetId)
                .distinct()
                .collect(Collectors.toList());

        // 2. 获取书评相关行为的书籍ID
        LambdaQueryWrapper<UserBehaviorLog> reviewQueryWrapper = new LambdaQueryWrapper<>();
        reviewQueryWrapper.ge(UserBehaviorLog::getCreateTime, since)
                .eq(UserBehaviorLog::getTargetType, "BOOK_REVIEW")
                .select(UserBehaviorLog::getTargetId);

        List<Long> reviewIds = userBehaviorLogMapper.selectList(reviewQueryWrapper).stream()
                .map(UserBehaviorLog::getTargetId)
                .distinct()
                .collect(Collectors.toList());

        // 3. 通过书评ID获取对应的书籍ID
        if (!reviewIds.isEmpty()) {
            LambdaQueryWrapper<BookReview> reviewWrapper = new LambdaQueryWrapper<>();
            reviewWrapper.in(BookReview::getId, reviewIds)
                    .select(BookReview::getBookId);

            List<Long> bookIdsFromReviews = reviewUserMapper.selectList(reviewWrapper).stream()
                    .map(BookReview::getBookId)
                    .distinct()
                    .collect(Collectors.toList());

            activeBookIds.addAll(bookIdsFromReviews);
        }

        // 4. 合并所有书籍ID
        activeBookIds.addAll(directBookIds);

        return activeBookIds.stream()
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * 计算书籍热度分数（本周热门）
     * 热度 = 收藏数 * 4 + 书评数 * 5 + 浏览数 * 0.5 + 书评点赞数 * 2 + 书评回复数 * 3
     */
    private Double calculateBookHotScore(Long bookId, LocalDateTime since) {
        // 1. 获取直接书籍行为
        LambdaQueryWrapper<UserBehaviorLog> bookQueryWrapper = new LambdaQueryWrapper<>();
        bookQueryWrapper.eq(UserBehaviorLog::getTargetId, bookId)
                .eq(UserBehaviorLog::getTargetType, "BOOK")
                .ge(UserBehaviorLog::getCreateTime, since);

        List<UserBehaviorLog> bookBehaviors = userBehaviorLogMapper.selectList(bookQueryWrapper);

        // 2. 获取该书书评相关行为
        LambdaQueryWrapper<BookReview> reviewQueryWrapper = new LambdaQueryWrapper<>();
        reviewQueryWrapper.eq(BookReview::getBookId, bookId)
                .ge(BookReview::getCreateTime, since)
                .select(BookReview::getId);

        List<Long> reviewIds = reviewUserMapper.selectList(reviewQueryWrapper).stream()
                .map(BookReview::getId)
                .distinct()
                .collect(Collectors.toList());

        List<UserBehaviorLog> reviewBehaviors = List.of();
        if (!reviewIds.isEmpty()) {
            LambdaQueryWrapper<UserBehaviorLog> reviewBehaviorWrapper = new LambdaQueryWrapper<>();
            reviewBehaviorWrapper.in(UserBehaviorLog::getTargetId, reviewIds)
                    .eq(UserBehaviorLog::getTargetType, "BOOK_REVIEW")
                    .ge(UserBehaviorLog::getCreateTime, since);

            reviewBehaviors = userBehaviorLogMapper.selectList(reviewBehaviorWrapper);
        }

        return rankingDomainService.calculateBookHotScore(bookBehaviors, reviewBehaviors);
    }

    /**
     * 计算书评热度分数
     * 基于用户行为日志统计书评的真实热度
     * 热度 = 书评点赞数 * 2 + 书评回复数 * 3
     */
    private Double calculateReviewHotScore(Long reviewId, LocalDateTime since) {
        // 获取该书评的所有用户行为
        LambdaQueryWrapper<UserBehaviorLog> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserBehaviorLog::getTargetId, reviewId)
                .eq(UserBehaviorLog::getTargetType, "BOOK_REVIEW")
                .ge(UserBehaviorLog::getCreateTime, since);

        List<UserBehaviorLog> reviewBehaviors = userBehaviorLogMapper.selectList(queryWrapper);

        return rankingDomainService.calculateReviewHotScore(reviewBehaviors);
    }



    /**
     * 更新书籍热度分数
     */
    private void updateBookHotScore(Long bookId, Double hotScore) {
        LambdaUpdateWrapper<Book> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Book::getId, bookId)
                .set(Book::getHotScore, hotScore)
                .set(Book::getHotScoreUpdateTime, LocalDateTime.now());

        bookUserMapper.update(null, updateWrapper);
    }

    /**
     * 更新书评热度分数
     */
    private void updateReviewHotScore(Long reviewId, Double hotScore) {
        LambdaUpdateWrapper<BookReview> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(BookReview::getId, reviewId)
                .set(BookReview::getHotScore, hotScore)
                .set(BookReview::getHotScoreUpdateTime, LocalDateTime.now());

        reviewUserMapper.update(null, updateWrapper);
    }
}
