package com.cc.booktalk.application.user.service.recommendation;

import com.cc.booktalk.interfaces.vo.user.rec.PersonalizedRecVO;

import java.util.List;

/**
 * 热门推荐服务
 */
public interface HotRecommendationService {

    /**
     * 获取热门书籍推荐
     *
     * @param limit 推荐数量
     * @return 热门推荐
     */
    List<PersonalizedRecVO> getHotRecommendations(Integer limit);

    /**
     * 刷新热门书籍缓存
     *
     * @param limit 缓存数量
     * @return 热门推荐
     */
    List<PersonalizedRecVO> refreshHotRecommendationsCache(Integer limit);
}
