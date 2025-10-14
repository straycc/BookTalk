package com.cc.talkpojo.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通知视图对象
 *
 * @author cc
 * @since 2025-10-12
 */
@Data
public class NotificationVO {

    /**
     * 通知ID
     */
    private Long id;

    /**
     * 接收用户ID
     */
    private Long userId;

    /**
     * 通知类型
     */
    private String type;

    /**
     * 通知类型描述
     */
    private String typeDesc;

    /**
     * 通知标题
     */
    private String title;

    /**
     * 通知内容
     */
    private String content;

    /**
     * 目标ID
     */
    private Long targetId;

    /**
     * 目标类型
     */
    private String targetType;

    /**
     * 发送者用户ID
     */
    private Long senderId;

    /**
     * 发送者用户名
     */
    private String senderName;

    /**
     * 发送者头像
     */
    private String senderAvatar;

    /**
     * 是否已读
     */
    private Boolean isRead;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 时间描述 (如: 刚刚、5分钟前)
     */
    private String timeDesc;
}