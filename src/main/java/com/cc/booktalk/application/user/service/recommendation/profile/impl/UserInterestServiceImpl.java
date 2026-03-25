package com.cc.booktalk.application.user.service.recommendation.profile.impl;

import com.cc.booktalk.domain.entity.recommendation.UserInterestTag;
import com.cc.booktalk.domain.enums.InterestType;
import com.cc.booktalk.infrastructure.persistence.user.mapper.recommendation.UserInterestTagMapper;
import com.cc.booktalk.application.user.service.recommendation.profile.UserInterestService;
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
     * @param interestType 兴趣类型
     * @param interestKey 兴趣键
     * @param interestScore 兴趣分数
     */
    @Override
    public void updateInterestScore(Long userId, InterestType interestType, String interestKey, Double interestScore) {
        if (userId == null || interestType == null || interestKey == null || interestKey.trim().isEmpty()
                || interestScore == null) {
            log.warn("更新用户兴趣参数不完整: userId={}, interestType={}, interestKey={}",
                    userId, interestType, interestKey);
            return;
        }

        // 查询是否已存在该兴趣标签
        UserInterestTag existingInterest = userInterestTagMapper.selectByUserAndInterest(
                userId,
                interestType,
                interestKey.trim()
        );

        if (existingInterest != null) {
            // 更新现有兴趣分数
            double newScore = existingInterest.getInterestScore() + interestScore;
            existingInterest.setInterestScore(newScore);
            existingInterest.setBehaviorCount(existingInterest.getBehaviorCount() + 1);
            existingInterest.setUpdateTime(LocalDateTime.now());
            userInterestTagMapper.updateById(existingInterest);

            log.debug("更新用户兴趣分数: userId={}, interestType={}, interestKey={}, 新分数={}",
                    userId, interestType, interestKey, newScore);
        } else {
            // 创建新的兴趣标签
            UserInterestTag newInterest = new UserInterestTag();
            newInterest.setUserId(userId);
            newInterest.setInterestType(interestType.name());
            newInterest.setInterestKey(interestKey.trim());
            newInterest.setInterestScore(interestScore);
            newInterest.setBehaviorCount(1);
            newInterest.setCreateTime(LocalDateTime.now());
            newInterest.setUpdateTime(LocalDateTime.now());

            userInterestTagMapper.insert(newInterest);

            log.debug("创建新用户兴趣: userId={}, interestType={}, interestKey={}, 分数={}",
                    userId, interestType, interestKey, interestScore);
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
