package com.cc.booktalk.domain.entity.recommendation;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.*;
import java.time.LocalDateTime;
import java.io.Serializable;

/**
 * 用户行为记录实体类
 * 用于记录用户的详细行为数据，是推荐系统的基础数据源
 *
 * @author cc
 * @since 2024-01-15
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("user_behavior_log")
public class UserBehaviorLog implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 目标对象ID（图书ID、书评ID等）
     */
    private Long targetId;

    /**
     * 目标类型：BOOK, REVIEW, USER等
     */
    private String targetType;

    /**
     * 行为类型：
     * - BOOK_VIEW: 浏览图书
     * - BOOK_LIKE: 点赞图书
     * - BOOK_COLLECT: 收藏图书
     * - BOOK_SCORE: 图书评分
     * - BOOK_REVIEW: 写书评
     * - REVIEW_LIKE: 点赞书评
     * - REVIEW_REPLY: 回复书评
     */
    private String behaviorType;

    /**
     * 行为分数
     * 根据行为类型设置不同的权重分数
     * 浏览:1.0, 点赞:3.0, 收藏:5.0, 评论:4.0, 评分:实际分值
     */
    private Double behaviorScore;

    /**
     * 额外数据（JSON格式）
     * 用于存储具体的评分值、评论内容等详细信息
     * 例如：{"score": 9, "review_content": "这本书很不错", "reply_content": "我也这么认为"}
     */
    private String extraData;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}