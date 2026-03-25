package com.cc.booktalk.application.user.service.recommendation;

import com.cc.booktalk.application.user.service.recommendation.model.RecommendationBookCandidate;

import java.util.List;

/**
 * 推荐排序服务
 */
public interface RecommendationRankService {

    /**
     * 为候选图书打分排序
     *
     * @param candidates 候选图书
     * @param limit 推荐数量
     * @return 排序后的候选图书
     */
    List<RecommendationBookCandidate> rankCandidates(List<RecommendationBookCandidate> candidates, Integer limit);
}
