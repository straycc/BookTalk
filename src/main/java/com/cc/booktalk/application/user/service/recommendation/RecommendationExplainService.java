package com.cc.booktalk.application.user.service.recommendation;

import com.cc.booktalk.application.user.service.recommendation.model.RecommendationBookCandidate;
import com.cc.booktalk.interfaces.vo.user.rec.PersonalizedRecVO;

import java.util.List;

/**
 * 推荐解释服务
 */
public interface RecommendationExplainService {

    /**
     * 将排序结果转换成对外推荐结果
     *
     * @param candidates 候选图书
     * @return 推荐结果
     */
    List<PersonalizedRecVO> buildRecommendations(List<RecommendationBookCandidate> candidates);
}
