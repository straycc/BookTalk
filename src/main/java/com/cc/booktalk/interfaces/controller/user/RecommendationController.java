package com.cc.booktalk.interfaces.controller.user;

import com.cc.booktalk.common.context.UserContext;
import com.cc.booktalk.common.result.Result;
import com.cc.booktalk.interfaces.vo.user.rec.PersonalizedRecVO;
import com.cc.booktalk.interfaces.vo.user.review.HotReviewVO;
import com.cc.booktalk.application.user.service.recommendation.RecommendationService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 个性化推荐控制器
 *
 * @author cc
 * @since 2025-10-17
 */
@Slf4j
@RestController
@RequestMapping("/user/recommendations")
@Api(tags = "个性化推荐接口")
public class RecommendationController {

    @Resource
    private RecommendationService recommendationService;

    /**
     * 获取个性化推荐
     */
    @GetMapping("/personalized")
    @ApiOperation("获取个性化推荐书籍")
    public Result<List<PersonalizedRecVO>> getPersonalizedRecommendations(
             @RequestParam(defaultValue = "10") Integer limit) {
        Long userId = UserContext.getUser().getId();

        log.info("获取个性化推荐: userId={}, limit={}", userId, limit);
        List<PersonalizedRecVO> recommendations = recommendationService.getPersonalizedRecommendations(userId, limit);
        log.info("个性化推荐完成: userId={}, 推荐数量={}", userId, recommendations.size());
        return Result.success(recommendations);
    }

    /**
     * 获取基于内容的推荐
     */
    @GetMapping("/content-based")
    @ApiOperation("获取基于内容的推荐书籍")
    public Result<List<PersonalizedRecVO>> getContentBasedRecommendations(
            @RequestParam(defaultValue = "10") Integer limit) {
        Long userId = UserContext.getUser().getId();

        log.info("获取基于内容的推荐: userId={}, limit={}", userId, limit);
        List<PersonalizedRecVO> recommendations = recommendationService.getContentBasedRecommendations(userId, limit);

        log.info("基于内容的推荐完成: userId={}, 推荐数量={}", userId, recommendations.size());
        return Result.success(recommendations);

    }

    /**
     * 获取协同过滤推荐
     */
    @GetMapping("/collaborative")
    @ApiOperation("获取协同过滤推荐书籍")
    public Result<List<PersonalizedRecVO>> getCollaborativeRecommendations(
            @RequestParam(defaultValue = "10") Integer limit) {
            Long userId = UserContext.getUser().getId();

            log.info("获取协同过滤推荐: userId={}, limit={}", userId, limit);
            List<PersonalizedRecVO> recommendations = recommendationService.getCollaborativeRecommendations(userId, limit);
            log.info("协同过滤推荐完成: userId={}, 推荐数量={}", userId, recommendations.size());
            return Result.success(recommendations);
    }

    /**
     * 获取热门推荐
     */
    @GetMapping("/hot")
    @ApiOperation("获取热门推荐书籍")
    public Result<List<PersonalizedRecVO>> getHotRecommendations(
            @ApiParam(value = "推荐数量限制", example = "10") @RequestParam(defaultValue = "10") Integer limit) {

            log.info("获取热门推荐: limit={}", limit);
            List<PersonalizedRecVO> recommendations = recommendationService.getHotRecommendations(limit);
            log.info("热门推荐完成: 推荐数量={}", recommendations.size());
            return Result.success(recommendations);
    }

    /**
     * 获取热门书评推荐（首页推荐区）
     */
    @GetMapping("/hot-reviews")
    @ApiOperation("获取热门书评推荐")
    public Result<List<HotReviewVO>> getHotReviewRecommendations(
            @ApiParam(value = "时间周期: daily/weekly/monthly 或 24h/7d/30d", example = "7d")
            @RequestParam(defaultValue = "7d") String period,
            @ApiParam(value = "推荐数量限制", example = "6")
            @RequestParam(defaultValue = "6") Integer limit) {
        log.info("获取热门书评推荐: period={}, limit={}", period, limit);
        List<HotReviewVO> recommendations = recommendationService.getHotReviewRecommendations(period, limit);
        log.info("热门书评推荐完成: 数量={}", recommendations.size());
        return Result.success(recommendations);
    }

    /**
     * 清除用户推荐缓存
     */
    @DeleteMapping("/cache/me")
    @ApiOperation("清除用户推荐缓存")
    public Result<String> clearRecommendationCache() {
        Long userId = UserContext.getUser().getId();
        log.info("清除用户推荐缓存: userId={}", userId);
        recommendationService.clearRecommendationCache(userId);
        log.info("用户推荐缓存清除完成: userId={}", userId);
        return Result.success("缓存清除成功");
    }
}
