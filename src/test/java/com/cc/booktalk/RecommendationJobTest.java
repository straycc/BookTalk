package com.cc.booktalk;

import com.cc.booktalk.application.user.service.rank.BookRankingRefreshService;
import com.cc.booktalk.application.user.service.rank.ReviewRankingRefreshService;
import com.cc.booktalk.common.constant.RedisCacheConstant;
import com.cc.booktalk.common.redis.RedisCacheUtils;
import com.cc.booktalk.interfaces.schedule.RecommendationJob;
import com.cc.booktalk.interfaces.vo.user.ranking.BookRankingVO;
import com.cc.booktalk.interfaces.vo.user.review.HotReviewVO;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import javax.annotation.Resource;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "booktalk.websocket.enabled=false"
)
public class RecommendationJobTest {


    @Resource
    private RecommendationJob recommendationJob;

    @Test
    public void hotRecommendationJobTest() {
        recommendationJob.updateHotRecommendations();
    }



    @Resource
    private BookRankingRefreshService bookRankingRefreshService;

    @Resource
    private ReviewRankingRefreshService reviewRankingRefreshService;

    @Resource
    private RedisCacheUtils redisCacheUtils;

    @Resource(name = "customObjectRedisTemplate")
    private RedisTemplate<String, Object> redisTemplate;

    @Test
    public void refreshAllRankings(){
        // 执行榜单预热
        reviewRankingRefreshService.refreshHotReviewsRanking();
        bookRankingRefreshService.refreshHotBooksRanking();
        bookRankingRefreshService.refreshNewBooksRanking();
        bookRankingRefreshService.refreshBookRatingRanking();

        // 校验Redis缓存已写入
        List<BookRankingVO> hotBooks = redisCacheUtils.getCacheDataLimit(
                RedisCacheConstant.RANKING_HOT_BOOKS_PREFIX + "weekly", 10, BookRankingVO.class);
        List<BookRankingVO> ratingBooks = redisCacheUtils.getCacheDataLimit(
                RedisCacheConstant.RANKING_BOOKS + ":book_rating:weekly", 10, BookRankingVO.class);
        List<HotReviewVO> hotReviews = redisCacheUtils.getCacheDataLimit(
                RedisCacheConstant.RANKING_HOT_REVIEWS_PREFIX + "weekly", 10, HotReviewVO.class);

        assertFalse(hotBooks.isEmpty(), "hot_books 榜单预热失败");
        assertFalse(ratingBooks.isEmpty(), "book_rating 榜单预热失败");
        assertTrue(Boolean.TRUE.equals(
                        redisTemplate.hasKey(RedisCacheConstant.RANKING_BOOKS + ":new_books:weekly")),
                "new_books 榜单预热失败（未写入缓存键）");
        assertFalse(hotReviews.isEmpty(), "hot_reviews 榜单预热失败");
    }




}
