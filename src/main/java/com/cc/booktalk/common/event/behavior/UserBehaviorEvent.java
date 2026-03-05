package com.cc.booktalk.common.event.behavior;

import lombok.*;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * 用户行为数据传输对象
 * 用于接收前端传递的用户行为数据，进行参数验证和数据转换
 *
 * @author cc
 * @since 2024-01-15
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserBehaviorEvent {

    /**
     * 用户ID
     */
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /**
     * 目标对象ID
     */
    @NotNull(message = "目标对象ID不能为空")
    private Long targetId;

    /**
     * 目标类型：BOOK, REVIEW, USER等
     */
    @NotNull(message = "目标类型不能为空")
    private String targetType;

    /**
     * 行为类型：
     * - BOOK_VIEW: 浏览图书
     * - BOOK_COLLECT: 收藏图书
     * - BOOK_REVIEW: 写书评
     * - REVIEW_LIKE: 点赞书评
     * - REVIEW_REPLY: 回复书评
     */
    @NotNull(message = "行为类型不能为空")
    private String behaviorType;

    /**
     * 行为分数
     * 如果不提供，将根据行为类型使用默认分数
     */
    private Double behaviorScore;

    /**
     * 额外数据（可选）
     * JSON格式，用于存储具体的行为相关信息
     * 例如：评分值、评论内容、回复内容等
     */
    private String extraData;


    /**
     * 行为时间
     * 如果不提供，将使用当前时间
     */
    private LocalDateTime occurredAt;
}