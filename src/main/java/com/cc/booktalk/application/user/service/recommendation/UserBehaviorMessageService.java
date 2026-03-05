package com.cc.booktalk.application.user.service.recommendation;

import com.cc.booktalk.common.event.behavior.UserBehaviorEvent;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class UserBehaviorMessageService {

    @Resource
    private UserBehaviorService userBehaviorService;

    @Resource
    private UserBehaviorInterestService userBehaviorInterestService;

    public void processUserBehavior(UserBehaviorEvent behaviorDTO) {
        userBehaviorService.recordUserBehavior(behaviorDTO);
        userBehaviorInterestService.updateUserInterest(behaviorDTO);
    }
}
