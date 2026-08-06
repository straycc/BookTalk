package com.cc.booktalk.application.user.service.recommendation.behavior;

import com.cc.booktalk.common.event.behavior.UserBehaviorEvent;
import com.cc.booktalk.application.user.service.recommendation.RecommendationService;
import com.cc.booktalk.application.user.service.recommendation.behavior.UserBehaviorService;
import com.cc.booktalk.application.user.service.recommendation.profile.UserBehaviorInterestService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Service
public class UserBehaviorMessageService {

    @Resource
    private UserBehaviorService userBehaviorService;

    @Resource
    private UserBehaviorInterestService userBehaviorInterestService;

    @Resource
    private RecommendationService recommendationService;

    @Transactional(rollbackFor = Exception.class)
    public void processUserBehavior(UserBehaviorEvent behaviorDTO) {
        userBehaviorService.recordUserBehavior(behaviorDTO);
        userBehaviorInterestService.updateUserInterest(behaviorDTO);
        recommendationService.clearRecommendationCache(behaviorDTO.getUserId());
    }
}
