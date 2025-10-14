package com.cc.talkuser.controller;

import com.cc.talkcommon.result.Result;
import com.cc.talkpojo.dto.RankingQueryDTO;
import com.cc.talkpojo.result.PageResult;
import com.cc.talkpojo.vo.BookRankingVO;
import com.cc.talkpojo.vo.HotReviewVO;
import com.cc.talkserver.user.service.RankingService;
import com.cc.talkserver.schedule.RankingUpdateTask;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 榜单推荐控制器
 *
 * @author cc
 * @since 2025-10-13
 */
@Slf4j
@Api(tags = "榜单推荐")
@RestController
@RequestMapping("/user/rankings")
public class RankingController {

    @Autowired
    private RankingService rankingService;

    @Autowired
    private RankingUpdateTask rankingUpdateTask;

    /**
     * 获取热门书评列表
     */
    @GetMapping("/hot-reviews")
    @ApiOperation("获取热门书评列表")
    public Result<PageResult<HotReviewVO>> getHotReviews(RankingQueryDTO queryDTO) {
        PageResult<HotReviewVO> result = rankingService.getHotReviews(queryDTO);
        return Result.success(result);
    }

    /**
     * 获取热门书评Top N
     */
    @GetMapping("/hot-reviews/top")
    @ApiOperation("获取热门书评Top N")
    public Result<List<HotReviewVO>> getHotReviewsTopN(
            @ApiParam(value = "时间周期", defaultValue = "weekly") @RequestParam(defaultValue = "weekly") String period,
            @ApiParam(value = "数量限制", defaultValue = "10") @RequestParam(defaultValue = "10") Integer limit) {
        List<HotReviewVO> result = rankingService.getHotReviewsTopN(period, limit);
        return Result.success(result);
    }

    /**
     * 获取书籍榜单
     */
    @GetMapping("/books")
    @ApiOperation("获取书籍榜单")
    public Result<PageResult<BookRankingVO>> getBookRankings(RankingQueryDTO queryDTO) {
        PageResult<BookRankingVO> result = rankingService.getBookRankings(queryDTO);
        return Result.success(result);
    }

    /**
     * 获取书籍榜单Top N
     */
    @GetMapping("/books/top")
    @ApiOperation("获取书籍榜单Top N")
    public Result<List<BookRankingVO>> getBookRankingsTopN(
            @ApiParam(value = "榜单类型", defaultValue = "book_rating") @RequestParam(defaultValue = "book_rating") String rankingType,
            @ApiParam(value = "时间周期", defaultValue = "monthly") @RequestParam(defaultValue = "monthly") String period,
            @ApiParam(value = "数量限制", defaultValue = "10") @RequestParam(defaultValue = "10") Integer limit) {
        List<BookRankingVO> result = rankingService.getBookRankingTopN(rankingType, period, limit);
        return Result.success(result);
    }

    /**
     * 获取相似书籍推荐
     */
    @GetMapping("/books/{bookId}/similar")
    @ApiOperation("获取相似书籍推荐")
    public Result<List<BookRankingVO>> getSimilarBooks(
            @ApiParam("书籍ID") @PathVariable Long bookId,
            @ApiParam(value = "推荐数量", defaultValue = "5") @RequestParam(defaultValue = "5") Integer limit) {
        List<BookRankingVO> result = rankingService.getSimilarBooks(bookId, limit);
        return Result.success(result);
    }

    /**
     * 获取首页推荐数据
     */
    @GetMapping("/home")
    @ApiOperation("获取首页推荐数据")
    public Result<HomeRecommendationVO> getHomeRecommendations() {
        try {
            HomeRecommendationVO vo = new HomeRecommendationVO();

            // 热门书评周榜Top 5
            vo.setHotReviewsWeekly(rankingService.getHotReviewsTopN("weekly", 5));

            // 图书评分月榜Top 5
            vo.setBookRatingMonthly(rankingService.getBookRankingTopN("book_rating", "monthly", 5));

            // 热门讨论月榜Top 5
            vo.setHotDiscussionMonthly(rankingService.getBookRankingTopN("hot_discussion", "monthly", 5));

            // 新书推荐Top 5
            vo.setNewBooksMonthly(rankingService.getBookRankingTopN("new_books", "monthly", 5));

            return Result.success(vo);
        } catch (Exception e) {
            log.error("获取首页推荐数据失败", e);
            return Result.error("获取推荐数据失败");
        }
    }

    /**
     * 手动刷新榜单（管理员功能）
     */
    @PostMapping("/refresh")
    @ApiOperation("手动刷新榜单")
    public Result<String> refreshRankings() {
        try {
            rankingUpdateTask.manualUpdateAllRankings();
            return Result.success("榜单刷新成功");
        } catch (Exception e) {
            log.error("手动刷新榜单失败", e);
            return Result.error("榜单刷新失败");
        }
    }

    /**
     * 首页推荐数据VO
     */
    public static class HomeRecommendationVO {
        private List<HotReviewVO> hotReviewsWeekly;
        private List<BookRankingVO> bookRatingMonthly;
        private List<BookRankingVO> hotDiscussionMonthly;
        private List<BookRankingVO> newBooksMonthly;

        // getters and setters
        public List<HotReviewVO> getHotReviewsWeekly() {
            return hotReviewsWeekly;
        }

        public void setHotReviewsWeekly(List<HotReviewVO> hotReviewsWeekly) {
            this.hotReviewsWeekly = hotReviewsWeekly;
        }

        public List<BookRankingVO> getBookRatingMonthly() {
            return bookRatingMonthly;
        }

        public void setBookRatingMonthly(List<BookRankingVO> bookRatingMonthly) {
            this.bookRatingMonthly = bookRatingMonthly;
        }

        public List<BookRankingVO> getHotDiscussionMonthly() {
            return hotDiscussionMonthly;
        }

        public void setHotDiscussionMonthly(List<BookRankingVO> hotDiscussionMonthly) {
            this.hotDiscussionMonthly = hotDiscussionMonthly;
        }

        public List<BookRankingVO> getNewBooksMonthly() {
            return newBooksMonthly;
        }

        public void setNewBooksMonthly(List<BookRankingVO> newBooksMonthly) {
            this.newBooksMonthly = newBooksMonthly;
        }
    }
}