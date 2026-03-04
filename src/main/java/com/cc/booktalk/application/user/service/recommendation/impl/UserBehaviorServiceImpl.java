package com.cc.booktalk.application.user.service.recommendation.impl;

import com.cc.booktalk.entity.dto.behavior.UserBehaviorDTO;
import com.cc.booktalk.entity.entity.recommendation.UserBehaviorLog;
import com.cc.booktalk.infrastructure.persistence.user.mapper.recommendation.UserBehaviorLogMapper;
import com.cc.booktalk.application.user.service.recommendation.UserBehaviorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户行为服务实现类
 *
 * @author cc
 * @since 2024-01-15
 */
@Slf4j
@Service
public class UserBehaviorServiceImpl implements UserBehaviorService {

    @Resource
    private UserBehaviorLogMapper userBehaviorLogMapper;

    /**
     * 记录用户行为数据
     * @param behaviorDTO 行为数据
     */
    @Override
    public void recordUserBehavior(UserBehaviorDTO behaviorDTO) {
        try {
            // 转换DTO为实体
            UserBehaviorLog behaviorLog = convertToEntity(behaviorDTO);

            // 保存到数据库
            userBehaviorLogMapper.insert(behaviorLog);

            log.debug("用户行为记录成功: userId={}, behaviorType={}, targetId={}",
                     behaviorDTO.getUserId(), behaviorDTO.getBehaviorType(), behaviorDTO.getTargetId());

        } catch (Exception e) {
            log.error("记录用户行为失败: userId={}, behaviorType={}",
                     behaviorDTO.getUserId(), behaviorDTO.getBehaviorType(), e);
            // 不抛出异常，避免影响主业务流程
        }
    }

    /**
     * 将DTO转换为实体类
     */
    private UserBehaviorLog convertToEntity(UserBehaviorDTO behaviorDTO) {
        UserBehaviorLog behaviorLog = new UserBehaviorLog();
        BeanUtils.copyProperties(behaviorDTO, behaviorLog);

        // 设置创建时间
        if (behaviorLog.getCreateTime() == null) {
            behaviorLog.setCreateTime(LocalDateTime.now());
        }

        // 设置默认行为分数
        if (behaviorLog.getBehaviorScore() == null) {
            behaviorLog.setBehaviorScore(getDefaultBehaviorScore(behaviorDTO.getBehaviorType()));
        }

        return behaviorLog;
    }

    /**
     * 获取默认行为分数
     */
    private Double getDefaultBehaviorScore(String behaviorType) {
        switch (behaviorType) {
            case "BOOK_VIEW":
                return 1.0;    // 浏览书籍 - 基础兴趣
            case "BOOK_COLLECT":
                return 5.0;    // 收藏书籍 - 强兴趣
            case "BOOK_REVIEW":
                return 4.0;    // 评论书籍 - 较强兴趣
            case "REVIEW_LIKE":
                return 2.0;    // 点赞评论 - 中等兴趣
            case "REVIEW_REPLY":
                return 3.0;    // 回复评论 - 较强兴趣
            default:
                return 1.0;    // 默认基础兴趣
        }
    }

    /**
     * 获取活跃用户列表
     *
     * @param days 最近天数
     * @param minActions 最小行为次数
     * @return 活跃用户ID列表
     */
    @Override
    public List<Long> getActiveUsers(int days, int minActions) {
        try {
            return userBehaviorLogMapper.getActiveUsers(days, minActions);
        } catch (Exception e) {
            log.error("获取活跃用户失败: days={}, minActions={}", days, minActions, e);
            return List.of();
        }
    }
}