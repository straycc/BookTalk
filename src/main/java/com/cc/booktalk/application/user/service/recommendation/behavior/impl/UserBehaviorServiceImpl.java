package com.cc.booktalk.application.user.service.recommendation.behavior.impl;

import com.cc.booktalk.common.event.behavior.UserBehaviorEvent;
import com.cc.booktalk.domain.entity.recommendation.UserBehaviorLog;
import com.cc.booktalk.infrastructure.persistence.user.mapper.recommendation.UserBehaviorLogMapper;
import com.cc.booktalk.application.user.service.recommendation.behavior.UserBehaviorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;

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
    public void recordUserBehavior(UserBehaviorEvent behaviorDTO) {
        if (behaviorDTO == null || behaviorDTO.getUserId() == null || behaviorDTO.getTargetId() == null
                || behaviorDTO.getBehaviorType() == null || behaviorDTO.getBehaviorType().isBlank()) {
            throw new IllegalArgumentException("用户行为事件不完整");
        }
        if (behaviorDTO.getBehaviorScore() == null) {
            behaviorDTO.setBehaviorScore(getDefaultBehaviorScore(behaviorDTO.getBehaviorType()));
        }
        UserBehaviorLog behaviorLog = convertToEntity(behaviorDTO);
        userBehaviorLogMapper.insert(behaviorLog);
        log.debug("用户行为记录成功: userId={}, behaviorType={}, targetId={}",
                behaviorDTO.getUserId(), behaviorDTO.getBehaviorType(), behaviorDTO.getTargetId());
    }

    /**
     * 将DTO转换为实体类
     */
    private UserBehaviorLog convertToEntity(UserBehaviorEvent behaviorEvent) {
        UserBehaviorLog behaviorLog = new UserBehaviorLog();
        BeanUtils.copyProperties(behaviorEvent, behaviorLog);

        // 事件发生时间字段映射到行为日志创建时间
        if (behaviorEvent.getOccurredAt() != null) {
            behaviorLog.setCreateTime(behaviorEvent.getOccurredAt());
        }

        // 设置创建时间
        if (behaviorLog.getCreateTime() == null) {
            behaviorLog.setCreateTime(LocalDateTime.now());
        }

        // 设置默认行为分数
        if (behaviorLog.getBehaviorScore() == null) {
            behaviorLog.setBehaviorScore(getDefaultBehaviorScore(behaviorEvent.getBehaviorType()));
        }

        return behaviorLog;
    }

    /**
     * 获取默认行为分数
     */
    private Double getDefaultBehaviorScore(String behaviorType) {
        if (behaviorType == null) {
            return 1.0;
        }
        String normalizedBehaviorType = behaviorType.trim().toUpperCase();
        switch (normalizedBehaviorType) {
            case "BOOK_VIEW":
                return 1.0;    // 浏览书籍 - 基础兴趣
            case "BOOK_LIKE":
                return 3.0;    // 点赞书籍 - 中等偏强兴趣
            case "BOOK_COLLECT":
                return 5.0;    // 收藏书籍 - 强兴趣
            case "BOOK_SCORE":
                return 4.0;    // 评分书籍 - 中等偏强兴趣（若传具体评分则以传入为准）
            case "BOOK_REVIEW":
                return 4.0;    // 评论书籍 - 较强兴趣
            case "REVIEW_LIKE":
                return 2.0;    // 点赞评论 - 中等兴趣
            case "REVIEW_COMMENT":
                return 2.5;    // 一级评论书评 - 中等兴趣
            case "REVIEW_REPLY":
                return 3.0;    // 回复评论 - 较强兴趣
            case "POST_VIEW":
                return 0.2;    // 浏览帖子 - 轻量互动
            case "POST_LIKE":
                return 2.0;    // 点赞帖子 - 中等兴趣
            case "POST_COMMENT":
                return 4.0;    // 评论帖子 - 高互动
            case "POST_REPLY":
                return 3.0;    // 回复帖子评论 - 较高互动
            default:
                return 1.0;    // 默认基础兴趣
        }
    }

}
