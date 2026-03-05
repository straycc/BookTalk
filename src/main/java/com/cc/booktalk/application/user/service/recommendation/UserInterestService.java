package com.cc.booktalk.application.user.service.recommendation;

import com.cc.booktalk.domain.entity.recommendation.UserInterestTag;

import java.util.List;

/**
 * 用户兴趣服务接口
 * 计算和管理用户兴趣标签
 *
 * @author cc
 *
 */
public interface UserInterestService {

    /**
     * 计算用户兴趣标签
     * 基于用户行为数据计算用户的兴趣偏好
     *
     * @param userId 用户ID
     */
    void calculateUserInterests(Long userId);

    /**
     * 获取用户兴趣标签
     *
     * @param userId 用户ID
     * @return 用户兴趣标签列表
     */
    List<UserInterestTag> getUserInterests(Long userId);

    /**
     * 更新兴趣标签分数
     * 考虑时间衰减因子
     *
     * @param userId 用户ID
     * @param tagName 标签名称
     * @param interestScore 兴趣分数
     */
    void updateInterestScore(Long userId, String tagName, Double interestScore);
}