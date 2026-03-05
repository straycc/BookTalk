package com.cc.booktalk.application.user.service.recommendation;

import com.cc.booktalk.interfaces.vo.user.rec.PersonalizedRecVO;
import com.cc.booktalk.interfaces.vo.user.review.HotReviewVO;
import java.util.List;

/**
 * 个性化推荐服务接口
 *
 * @author cc
 * @since 2025-10-17
 */

public interface RecommendationService {

    /**
     * 获取个性化推荐书籍
     * 基于用户兴趣标签和行为历史推荐
     *
     * @param userId 用户ID
     * @param limit 推荐数量限制
     * @return 推荐书籍列表
     */
    List<PersonalizedRecVO> getPersonalizedRecommendations(Long userId, Integer limit);

    /**
     * 获取基于内容的推荐
     * 基于用户兴趣标签推荐相似内容
     *
     * @param userId 用户ID
     * @param limit 推荐数量限制
     * @return 推荐书籍列表
     */
    List<PersonalizedRecVO> getContentBasedRecommendations(Long userId, Integer limit);

    /**
     * 获取协同过滤推荐
     * 基于相似用户的行为推荐
     *
     * @param userId 用户ID
     * @param limit 推荐数量限制
     * @return 推荐书籍列表
     */
    List<PersonalizedRecVO> getCollaborativeRecommendations(Long userId, Integer limit);

    /**
     * 获取热门推荐
     * 基于整体热度推荐
     *
     * @param limit 推荐数量限制
     * @return 推荐书籍列表
     */
    List<PersonalizedRecVO> getHotRecommendations(Integer limit);

    /**
     * 刷新热门推荐缓存（定时任务调用）
     *
     * @param limit 缓存的推荐数量
     * @return 刷新后的热门推荐列表
     */
    List<PersonalizedRecVO> refreshHotRecommendationsCache(Integer limit);

    /**
     * 获取热门书评推荐（用于首页推荐区）
     *
     * @param period 时间周期: daily/weekly/monthly 或 24h/7d/30d
     * @param limit 推荐数量
     * @return 热门书评列表
     */
    List<HotReviewVO> getHotReviewRecommendations(String period, Integer limit);

    /**
     * 刷新热门书评推荐缓存（定时任务调用）
     *
     * @param period 时间周期: daily/weekly/monthly 或 24h/7d/30d
     * @param limit 缓存数量
     * @return 刷新后的热门书评列表
     */
    List<HotReviewVO> refreshHotReviewRecommendationsCache(String period, Integer limit);

    /**
     * 清除用户推荐缓存
     *
     * @param userId 用户ID
     */
    void clearRecommendationCache(Long userId);

}
