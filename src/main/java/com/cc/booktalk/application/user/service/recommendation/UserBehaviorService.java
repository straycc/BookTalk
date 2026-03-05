package com.cc.booktalk.application.user.service.recommendation;

import com.cc.booktalk.common.event.behavior.UserBehaviorEvent;

import java.util.List;

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

    /**
     * 获取活跃用户列表
     *
     * @param days 最近天数
     * @param minActions 最小行为次数
     * @return 活跃用户ID列表
     */
    List<Long> getActiveUsers(int days, int minActions);
}