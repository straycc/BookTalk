package com.cc.booktalk.interfaces.controller.user;
import com.cc.booktalk.common.result.Result;
import com.cc.booktalk.interfaces.dto.user.ranking.RankingQueryDTO;
import com.cc.booktalk.common.result.PageResult;
import com.cc.booktalk.interfaces.vo.user.ranking.BookRankingVO;
import com.cc.booktalk.interfaces.vo.user.review.HotReviewVO;
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
     * 获取完整书籍榜单（分页）
     */
    @GetMapping("/books")
    @ApiOperation("获取完整书籍榜单")
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
            @ApiParam(value = "榜单类型", defaultValue = "hot_books") @RequestParam(defaultValue = "hot_books") String rankingType,
            @ApiParam(value = "时间周期", defaultValue = "weekly") @RequestParam(defaultValue = "weekly") String period,
            @ApiParam(value = "数量限制", defaultValue = "10") @RequestParam(defaultValue = "10") Integer limit) {
        List<BookRankingVO> result = rankingService.getBookRankingTopN(rankingType, period, limit);
        return Result.success(result);
    }

    /**
     * 获取热门书评榜单Top N
     */
    @GetMapping("/hot-reviews/top")
    @ApiOperation("获取热门书评榜单Top N")
    public Result<List<HotReviewVO>> getHotReviewRankingTopN(
            @ApiParam(value = "时间周期", defaultValue = "weekly") @RequestParam(defaultValue = "weekly") String period,
            @ApiParam(value = "数量限制", defaultValue = "10") @RequestParam(defaultValue = "10") Integer limit) {
        List<HotReviewVO> result = rankingService.getHotReviewRankingTopN(period, limit);
        return Result.success(result);
    }
}
