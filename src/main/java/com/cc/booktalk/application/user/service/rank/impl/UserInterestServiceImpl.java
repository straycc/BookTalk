package com.cc.booktalk.application.user.service.rank.impl;

import com.cc.booktalk.domain.entity.recommendation.UserInterestTag;
import com.cc.booktalk.infrastructure.persistence.user.mapper.recommendation.UserInterestTagMapper;
import com.cc.booktalk.application.user.service.recommendation.UserInterestService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户兴趣服务实现类
 *
 * @author cc
 * @since 2024-01-15
 */
@Slf4j
@Service
public class UserInterestServiceImpl implements UserInterestService {

    @Resource
    private UserInterestTagMapper userInterestTagMapper;

    /**
     * 时间衰减因子（每天衰减的百分比）
     */
    private static final double TIME_DECAY_FACTOR = 0.95;


    /**
     * 计算用户兴趣标签
     * @param userId 用户ID
     */
    @Override
    public void calculateUserInterests(Long userId) {
        try {
            log.debug("开始计算用户兴趣: userId={}", userId);

            // 获取用户最近的行为数据
            List<UserInterestTag> existingInterests = userInterestTagMapper.selectByUserId(userId);

            // 对每个兴趣标签应用时间衰减
            for (UserInterestTag interest : existingInterests) {
                applyTimeDecay(interest);
                userInterestTagMapper.updateById(interest);
            }

            log.debug("用户兴趣计算完成: userId={}, 处理了{}个兴趣标签", userId, existingInterests.size());

        } catch (Exception e) {
            log.error("计算用户兴趣失败: userId={}", userId, e);
        }
    }

    /**
     * 获取用户兴趣标签
     * @param userId 用户ID
     * @return List<UserInterestTag>
     */
    @Override
    public List<UserInterestTag> getUserInterests(Long userId) {
        try {
            return userInterestTagMapper.selectByUserId(userId);
        } catch (Exception e) {
            log.error("获取用户兴趣失败: userId={}", userId, e);
            return null;
        }
    }

    /**
     * 更新用户兴趣标签
     * @param userId 用户ID
     * @param tagName 标签名称
     * @param interestScore 兴趣分数
     */
    @Override
    public void updateInterestScore(Long userId, String tagName, Double interestScore) {
        // 查询是否已存在该兴趣标签
        UserInterestTag existingInterest = userInterestTagMapper.selectByUserAndTag(userId, tagName);

        if (existingInterest != null) {
            // 更新现有兴趣分数
            double newScore = existingInterest.getInterestScore() + interestScore;
            existingInterest.setInterestScore(newScore);
            existingInterest.setBehaviorCount(existingInterest.getBehaviorCount() + 1);
            existingInterest.setUpdateTime(LocalDateTime.now());
            userInterestTagMapper.updateById(existingInterest);

            log.debug("更新用户兴趣分数: userId={}, tagName={}, 新分数={}",
                     userId, tagName, newScore);
        } else {
            // 创建新的兴趣标签
            UserInterestTag newInterest = new UserInterestTag();
            newInterest.setUserId(userId);
            newInterest.setTagName(tagName);
            newInterest.setInterestScore(interestScore);
            newInterest.setBehaviorCount(1);
            newInterest.setCreateTime(LocalDateTime.now());
            newInterest.setUpdateTime(LocalDateTime.now());

            userInterestTagMapper.insert(newInterest);

            log.debug("创建新用户兴趣: userId={}, tagName={}, 分数={}",
                     userId, tagName, interestScore);
        }
    }

    /**
     * 应用时间衰减
     * 根据最后更新时间计算衰减后的兴趣分数
     */
    private void applyTimeDecay(UserInterestTag interest) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lastUpdate = interest.getUpdateTime();

        if (lastUpdate != null) {
            // 计算距离上次更新的天数
            long daysDiff = java.time.Duration.between(lastUpdate, now).toDays();

            if (daysDiff > 0) {
                // 应用时间衰减公式：新分数 = 原分数 * (衰减因子 ^ 天数)
                double decayedScore = interest.getInterestScore() * Math.pow(TIME_DECAY_FACTOR, daysDiff);
                interest.setInterestScore(decayedScore);
                interest.setUpdateTime(now);

                log.debug("应用时间衰减: userId={}, 原分数={}, 天数={}, 衰减后分数={}",
                         interest.getUserId(), interest.getInterestScore(), daysDiff, decayedScore);
            }
        }
    }
}