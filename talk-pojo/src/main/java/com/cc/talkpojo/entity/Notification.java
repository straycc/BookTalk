package com.cc.talkpojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通知实体类
 *
 * @author cc
 * @since 2025-10-12
 */
@Data
@TableName("notification")
public class Notification {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 接收用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 通知类型
     * LIKE-点赞, COMMENT-评论, REPLY-回复, FOLLOW-关注, SYSTEM-系统
     */
    @TableField("type")
    private String type;

    /**
     * 通知标题
     */
    @TableField("title")
    private String title;

    /**
     * 通知内容
     */
    @TableField("content")
    private String content;

    /**
     * 目标ID (书评/评论ID)
     */
    @TableField("target_id")
    private Long targetId;

    /**
     * 目标类型
     * BOOK_REVIEW-书评, COMMENT-评论, USER-用户
     */
    @TableField("target_type")
    private String targetType;

    /**
     * 发送者用户ID
     */
    @TableField("sender_id")
    private Long senderId;

    /**
     * 发送者用户名
     */
    @TableField("sender_name")
    private String senderName;

    /**
     * 发送者头像
     */
    @TableField("sender_avatar")
    private String senderAvatar;

    /**
     * 是否已读
     */
    @TableField("is_read")
    private Boolean isRead;

    /**
     * 是否删除
     */
    @TableField("is_deleted")
    private Boolean isDeleted;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}