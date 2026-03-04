package com.cc.booktalk.interfaces.user.controller;
import com.cc.booktalk.common.result.Result;
import com.cc.booktalk.entity.dto.ranking.RankingQueryDTO;
import com.cc.booktalk.entity.result.PageResult;
import com.cc.booktalk.entity.vo.BookRankingVO;
import com.cc.booktalk.entity.vo.HotReviewVO;
import com.cc.booktalk.application.user.service.rank.RankingService;
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


    /**
     * 获取热门书评列表
     */
    @GetMapping("/hot-reviews")
    @ApiOperation("获取热门书评列表")
    public Result<List<HotReviewVO>> getHotReviews(RankingQueryDTO queryDTO) {
        List<HotReviewVO> result = rankingService.getHotReviews(queryDTO);
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
}