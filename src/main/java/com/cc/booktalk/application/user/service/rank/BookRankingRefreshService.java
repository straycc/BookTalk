package com.cc.booktalk.application.user.service.rank;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.cc.booktalk.common.constant.RedisCacheConstant;
import com.cc.booktalk.domain.entity.book.Book;
import com.cc.booktalk.domain.entity.recommendation.UserBehaviorLog;
import com.cc.booktalk.domain.entity.review.BookReview;
import com.cc.booktalk.domain.recommendation.HotBookRecDomain;
import com.cc.booktalk.domain.recommendation.HotReviewRecDomain;
import com.cc.booktalk.infrastructure.persistence.user.mapper.book.BookUserMapper;
import com.cc.booktalk.infrastructure.persistence.user.mapper.recommendation.UserBehaviorLogMapper;
import com.cc.booktalk.infrastructure.persistence.user.mapper.review.ReviewUserMapper;
import com.cc.booktalk.interfaces.vo.user.ranking.BookRankingVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class BookRankingRefreshService {

    private static final String TARGET_TYPE_BOOK = "BOOK";
    private static final String TARGET_TYPE_REVIEW = "REVIEW";
    private static final String RANKING_TYPE_HOT_BOOKS = "hot_books";
    private static final String RANKING_TYPE_BOOK_RATING = "book_rating";
    private static final String RANKING_TYPE_NEW_BOOKS = "new_books";
    private static final String PERIOD = "weekly";
    private static final int WINDOW_DAYS = 7;
    private static final int NEW_BOOK_WINDOW_DAYS = 30;
    private static final int RANK_LIMIT = 50;
    private static final int UPDATE_BATCH_SIZE = 100;
    private static final Duration CACHE_TTL = Duration.ofHours(2);

    @Resource
    private BookUserMapper bookUserMapper;

    @Resource
    private ReviewUserMapper reviewUserMapper;

    @Resource
    private UserBehaviorLogMapper userBehaviorLogMapper;

    @Resource(name = "customObjectRedisTemplate")
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private HotBookRecDomain hotBookRecDomain;

    @Resource
    private HotReviewRecDomain hotReviewRecDomain;

    /**
     * 完整链路：计算热度 -> 入库 -> 刷榜单缓存
     */
    public void refreshHotBooksRanking() {
        LocalDateTime since = LocalDateTime.now().minusDays(WINDOW_DAYS);
        calculateAndPersistBookHotScores(since);
        buildAndCacheHotBooksRanking(RANK_LIMIT);
    }

    /**
     * 构建评分榜缓存
     */
    public void refreshBookRatingRanking() {
        buildAndCacheBookRatingRanking(RANK_LIMIT);
    }

    /**
     * 构建新书榜缓存
     */
    public void refreshNewBooksRanking() {
        buildAndCacheNewBooksRanking(RANK_LIMIT);
    }

    public void calculateBookHotScores() {
        calculateAndPersistBookHotScores(LocalDateTime.now().minusDays(WINDOW_DAYS));
    }

    public void updateHotBooksToRedis() {
        buildAndCacheHotBooksRanking(RANK_LIMIT);
    }

    private void calculateAndPersistBookHotScores(LocalDateTime since) {
        try {
            log.info("开始计算书籍热度分数");
            List<Long> activeBookIds = listActiveBookIds(since);
            if (activeBookIds.isEmpty()) {
                log.info("最近{}天无活跃书籍，跳过热度计算", WINDOW_DAYS);
                return;
            }

            int updatedCount = 0;
            for (int i = 0; i < activeBookIds.size(); i += UPDATE_BATCH_SIZE) {
                int end = Math.min(i + UPDATE_BATCH_SIZE, activeBookIds.size());
                for (Long bookId : activeBookIds.subList(i, end)) {
                    try {
                        Double hotScore = computeBookHotScore(bookId, since);
                        LambdaUpdateWrapper<Book> update = new LambdaUpdateWrapper<>();
                        update.eq(Book::getId, bookId)
                                .set(Book::getHotScore, hotScore)
                                .set(Book::getHotScoreUpdateTime, LocalDateTime.now());
                        bookUserMapper.update(null, update);
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

    private List<Long> listActiveBookIds(LocalDateTime since) {
        List<Long> activeBookIds = new ArrayList<>();

        LambdaQueryWrapper<UserBehaviorLog> bookActionQuery = new LambdaQueryWrapper<>();
        bookActionQuery.ge(UserBehaviorLog::getCreateTime, since)
                .eq(UserBehaviorLog::getTargetType, TARGET_TYPE_BOOK)
                .select(UserBehaviorLog::getTargetId);
        activeBookIds.addAll(userBehaviorLogMapper.selectList(bookActionQuery).stream()
                .map(UserBehaviorLog::getTargetId)
                .distinct()
                .collect(Collectors.toList()));

        LambdaQueryWrapper<UserBehaviorLog> reviewActionQuery = new LambdaQueryWrapper<>();
        reviewActionQuery.ge(UserBehaviorLog::getCreateTime, since)
                .eq(UserBehaviorLog::getTargetType, TARGET_TYPE_REVIEW)
                .select(UserBehaviorLog::getTargetId);
        List<Long> reviewIds = userBehaviorLogMapper.selectList(reviewActionQuery).stream()
                .map(UserBehaviorLog::getTargetId)
                .distinct()
                .collect(Collectors.toList());

        if (!reviewIds.isEmpty()) {
            LambdaQueryWrapper<BookReview> reviewQuery = new LambdaQueryWrapper<>();
            reviewQuery.in(BookReview::getId, reviewIds).select(BookReview::getBookId);
            activeBookIds.addAll(reviewUserMapper.selectList(reviewQuery).stream()
                    .map(BookReview::getBookId)
                    .distinct()
                    .collect(Collectors.toList()));
        }

        return activeBookIds.stream().distinct().collect(Collectors.toList());
    }

    private Double computeBookHotScore(Long bookId, LocalDateTime since) {
        LambdaQueryWrapper<UserBehaviorLog> bookBehaviorQuery = new LambdaQueryWrapper<>();
        bookBehaviorQuery.eq(UserBehaviorLog::getTargetId, bookId)
                .eq(UserBehaviorLog::getTargetType, TARGET_TYPE_BOOK)
                .ge(UserBehaviorLog::getCreateTime, since);
        List<UserBehaviorLog> bookBehaviors = userBehaviorLogMapper.selectList(bookBehaviorQuery);

        LambdaQueryWrapper<BookReview> reviewQuery = new LambdaQueryWrapper<>();
        reviewQuery.eq(BookReview::getBookId, bookId)
                .ge(BookReview::getCreateTime, since)
                .select(BookReview::getId);
        List<Long> reviewIds = reviewUserMapper.selectList(reviewQuery).stream()
                .map(BookReview::getId)
                .distinct()
                .collect(Collectors.toList());

        List<UserBehaviorLog> reviewBehaviors = List.of();
        if (!reviewIds.isEmpty()) {
            LambdaQueryWrapper<UserBehaviorLog> reviewBehaviorQuery = new LambdaQueryWrapper<>();
            reviewBehaviorQuery.in(UserBehaviorLog::getTargetId, reviewIds)
                    .eq(UserBehaviorLog::getTargetType, TARGET_TYPE_REVIEW)
                    .ge(UserBehaviorLog::getCreateTime, since);
            reviewBehaviors = userBehaviorLogMapper.selectList(reviewBehaviorQuery);
        }

        LocalDateTime now = LocalDateTime.now();
        return hotBookRecDomain.calculateHotScore(bookBehaviors, now)
                + hotReviewRecDomain.calculateHotScore(reviewBehaviors, now);
    }

    private void buildAndCacheHotBooksRanking(int limit) {
        try {
            LambdaQueryWrapper<Book> rankingQuery = new LambdaQueryWrapper<>();
            rankingQuery.orderByDesc(Book::getHotScore)
                    .orderByDesc(Book::getHotScoreUpdateTime)
                    .last("LIMIT " + limit);
            List<Book> books = bookUserMapper.selectList(rankingQuery);

            List<BookRankingVO> rankings = new ArrayList<>(books.size());
            for (int i = 0; i < books.size(); i++) {
                rankings.add(toRankingVO(books.get(i), i + 1, RANKING_TYPE_HOT_BOOKS));
            }

            String redisKey = RedisCacheConstant.RANKING_HOT_BOOKS_PREFIX + PERIOD;
            redisTemplate.opsForValue().set(redisKey, rankings, CACHE_TTL);
            log.info("热门书籍榜单缓存更新完成: key={}, count={}", redisKey, rankings.size());
        } catch (Exception e) {
            log.error("更新热门书籍榜单缓存失败", e);
        }
    }

    private void buildAndCacheBookRatingRanking(int limit) {
        try {
            LambdaQueryWrapper<Book> rankingQuery = new LambdaQueryWrapper<>();
            rankingQuery.ge(Book::getScoreCount, 20)
                    .orderByDesc(Book::getAverageScore)
                    .orderByDesc(Book::getScoreCount)
                    .orderByDesc(Book::getCreateTime)
                    .last("LIMIT " + limit);
            List<Book> books = bookUserMapper.selectList(rankingQuery);

            List<BookRankingVO> rankings = new ArrayList<>(books.size());
            for (int i = 0; i < books.size(); i++) {
                rankings.add(toRankingVO(books.get(i), i + 1, RANKING_TYPE_BOOK_RATING));
            }

            String redisKey = RedisCacheConstant.RANKING_BOOKS + ":" + RANKING_TYPE_BOOK_RATING + ":" + PERIOD;
            redisTemplate.opsForValue().set(redisKey, rankings, CACHE_TTL);
            log.info("评分书籍榜单缓存更新完成: key={}, count={}", redisKey, rankings.size());
        } catch (Exception e) {
            log.error("更新评分书籍榜单缓存失败", e);
        }
    }

    private void buildAndCacheNewBooksRanking(int limit) {
        try {
            LocalDateTime cutoff = LocalDateTime.now().minusDays(NEW_BOOK_WINDOW_DAYS);
            LambdaQueryWrapper<Book> rankingQuery = new LambdaQueryWrapper<>();
            rankingQuery.ge(Book::getCreateTime, cutoff)
                    .orderByDesc(Book::getCreateTime)
                    .orderByDesc(Book::getHotScore)
                    .last("LIMIT " + limit);
            List<Book> books = bookUserMapper.selectList(rankingQuery);

            List<BookRankingVO> rankings = new ArrayList<>(books.size());
            for (int i = 0; i < books.size(); i++) {
                rankings.add(toRankingVO(books.get(i), i + 1, RANKING_TYPE_NEW_BOOKS));
            }

            String redisKey = RedisCacheConstant.RANKING_BOOKS + ":" + RANKING_TYPE_NEW_BOOKS + ":" + PERIOD;
            redisTemplate.opsForValue().set(redisKey, rankings, CACHE_TTL);
            log.info("新书榜单缓存更新完成: key={}, count={}", redisKey, rankings.size());
        } catch (Exception e) {
            log.error("更新新书榜单缓存失败", e);
        }
    }

    private BookRankingVO toRankingVO(Book book, int rank, String rankingType) {
        BookRankingVO vo = new BookRankingVO();
        BeanUtils.copyProperties(book, vo);
        vo.setBookId(book.getId());
        vo.setBookTitle(book.getTitle());
        vo.setBookCover(book.getCoverUrl());
        vo.setAvgRating(book.getAverageScore());
        vo.setRatingCount(book.getScoreCount());
        vo.setReadCount(book.getFavoriteCount() != null ? book.getFavoriteCount().longValue() : 0L);
        vo.setPublishTime(book.getCreateTime());
        vo.setHotScore(book.getHotScore());
        vo.setCategoryId(book.getCategoryId());
        vo.setRank(rank);
        vo.setRankingType(rankingType);
        vo.setRankingPeriod(PERIOD);
        return vo;
    }
}
