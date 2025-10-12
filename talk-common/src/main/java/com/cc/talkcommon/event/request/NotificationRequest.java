package com.cc.talkcommon.event.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.UtilityClass;

/**
 * 通知请求统一参数类
 *
 * @author cc
 * @since 2025-10-12
 */
@Data
@Builder
@AllArgsConstructor
public class NotificationRequest {

    /**
     * 接收用户ID
     */
    private Long userId;

    /**
     * 通知类型
     */
    private String type;

    /**
     * 目标ID (书评/评论ID等)
     */
    private Long targetId;

    /**
     * 目标类型
     */
    private String targetType;

    /**
     * 发送者信息
     */
    private SenderInfo sender;

    /**
     * 自定义标题 (可选)
     */
    private String title;

    /**
     * 自定义内容 (可选)
     */
    private String content;

    /**
     * 通知类型枚举
     */
    @UtilityClass
    public static class NotificationType {
        public static final String LIKE = "LIKE";
        public static final String COMMENT = "COMMENT";
        public static final String REPLY = "REPLY";
        public static final String FOLLOW = "FOLLOW";
        public static final String SYSTEM = "SYSTEM";
    }

    /**
     * 目标类型枚举
     */
    @UtilityClass
    public static class TargetType {
        public static final String BOOK_REVIEW = "BOOK_REVIEW";
        public static final String COMMENT = "COMMENT";
        public static final String USER = "USER";
    }

    /**
     * 发送者信息
     */
    @Data
    @AllArgsConstructor
    public static class SenderInfo {
        private Long id;
        private String name;
        private String avatar;
    }

    /**
     * 快速创建点赞通知请求
     */
    public static NotificationRequest like(Long targetUserId, Long targetId, String targetType, Long senderId, String senderName, String senderAvatar) {
        return NotificationRequest.builder()
                .userId(targetUserId)
                .type(NotificationType.LIKE)
                .targetId(targetId)
                .targetType(targetType)
                .sender(new SenderInfo(senderId, senderName, senderAvatar))
                .title("收到新点赞")
                .build();
    }

    /**
     * 快速创建评论通知请求
     */
    public static NotificationRequest comment(Long targetUserId, Long targetId, String targetType, String commentContent, Long senderId, String senderName, String senderAvatar) {
        String title = "收到新评论";
        String content = commentContent.length() > 30 ? commentContent.substring(0, 30) + "..." : commentContent;

        return NotificationRequest.builder()
                .userId(targetUserId)
                .type(NotificationType.COMMENT)
                .targetId(targetId)
                .targetType(targetType)
                .sender(new SenderInfo(senderId, senderName, senderAvatar))
                .title(title)
                .content(content)
                .build();
    }

    /**
     * 快速创建关注通知请求
     */
    public static NotificationRequest follow(Long targetUserId, Long senderId, String senderName, String senderAvatar) {
        return NotificationRequest.builder()
                .userId(targetUserId)
                .type(NotificationType.FOLLOW)
                .targetId(senderId)
                .targetType(TargetType.USER)
                .sender(new SenderInfo(senderId, senderName, senderAvatar))
                .title("收到新关注")
                .content("关注了你")
                .build();
    }

    /**
     * 快速创建系统通知请求
     */
    public static NotificationRequest system(Long userId, String title, String content) {
        return NotificationRequest.builder()
                .userId(userId)
                .type(NotificationType.SYSTEM)
                .title(title)
                .content(content)
                .targetType(TargetType.USER)
                .build();
    }
}