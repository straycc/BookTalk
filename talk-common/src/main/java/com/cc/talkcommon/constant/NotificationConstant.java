package com.cc.talkcommon.constant;

/**
 * 通知相关常量
 *
 * @author cc
 * @since 2025-10-12
 */
public class NotificationConstant {

    // 通知类型
    public static final String NOTIFICATION_TYPE_LIKE = "LIKE";
    public static final String NOTIFICATION_TYPE_COMMENT = "COMMENT";
    public static final String NOTIFICATION_TYPE_REPLY = "REPLY";
    public static final String NOTIFICATION_TYPE_FOLLOW = "FOLLOW";
    public static final String NOTIFICATION_TYPE_SYSTEM = "SYSTEM";

    // 目标类型
    public static final String TARGET_TYPE_BOOK_REVIEW = "BOOK_REVIEW";
    public static final String TARGET_TYPE_COMMENT = "COMMENT";
    public static final String TARGET_TYPE_USER = "USER";

    // 通知消息模板
    public static final String TEMPLATE_LIKE_REVIEW = "赞了你的书评";
    public static final String TEMPLATE_LIKE_COMMENT = "赞了你的评论";
    public static final String TEMPLATE_COMMENT_REVIEW = "评论了你的书评：{content}";
    public static final String TEMPLATE_REPLY_COMMENT = "回复了你的评论：{content}";
    public static final String TEMPLATE_FOLLOW_USER = "关注了你";
    public static final String TEMPLATE_SYSTEM_NOTICE = "系统通知：{title}";

    // WebSocket 相关
    public static final String WS_NOTIFICATION_PATH = "/ws/notification";
    public static final String WS_TOPIC_NOTIFICATION = "/topic/notification/";

    // 业务异常消息
    public static final String NOTIFICATION_NOT_EXIST = "通知不存在";
    public static final String NOTIFICATION_SETTING_UPDATE_SUCCESS = "通知设置更新成功";
    public static final String NOTIFICATION_BATCH_READ_SUCCESS = "批量标记已读成功";
    public static final String NOTIFICATION_BATCH_DELETE_SUCCESS = "批量删除成功";
}