package com.cc.booktalk.application.user.service.recommendation;

import com.cc.booktalk.application.user.service.recommendation.model.RecommendationBookCandidate;

import java.util.List;

/**
 * 推荐过滤服务
 */
public interface RecommendationFilterService {

    /**
     * 过滤用户已消费或应排除的候选图书
     *
     * @param userId 用户ID
     * @param candidates 候选图书
     * @return 过滤后的图书
     */
    List<RecommendationBookCandidate> filterCandidates(Long userId, List<RecommendationBookCandidate> candidates);
}
