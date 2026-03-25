package com.cc.booktalk.application.user.service.recommendation;

import com.cc.booktalk.application.user.service.recommendation.model.RecommendationBookCandidate;
import com.cc.booktalk.application.user.service.recommendation.model.UserProfileSnapshot;

import java.util.List;

/**
 * 推荐召回服务
 */
public interface RecommendationRecallService {

    /**
     * 基于用户画像进行多路召回
     *
     * @param snapshot 用户画像快照
     * @param limit 推荐数量
     * @return 候选图书
     */
    List<RecommendationBookCandidate> recallCandidates(UserProfileSnapshot snapshot, Integer limit);
}
