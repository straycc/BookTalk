package com.cc.booktalk.application.user.service.recommendation.behavior;

import com.cc.booktalk.common.event.behavior.UserBehaviorEvent;


/**
 * 用户行为服务接口
 *
 * @author cc
 * @since 2025-10-17
 */
public interface UserBehaviorService {

    /**
     * 记录用户行为
     *
     * @param behaviorDTO 行为数据
     */
    void recordUserBehavior(UserBehaviorEvent behaviorDTO);

}
