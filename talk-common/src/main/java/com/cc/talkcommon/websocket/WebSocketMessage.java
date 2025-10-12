package com.cc.talkcommon.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * WebSocket消息实体
 *
 * @author cc
 * @since 2025-10-12
 */
@Data
@Builder
@AllArgsConstructor
public class WebSocketMessage<T> {

    /**
     * 消息类型
     */
    private String type;

    /**
     * 接收用户ID
     */
    private Long userId;

    /**
     * 消息内容
     */
    private T data;

    /**
     * 时间戳
     */
    private Long timestamp;

    /**
     * 消息类型常量
     */
    public static class MessageType {
        public static final String NEW_NOTIFICATION = "NEW_NOTIFICATION";
        public static final String UNREAD_COUNT_UPDATE = "UNREAD_COUNT_UPDATE";
        public static final String NOTIFICATION_READ = "NOTIFICATION_READ";
        public static final String BATCH_OPERATION_RESULT = "BATCH_OPERATION_RESULT";
        public static final String HEARTBEAT = "HEARTBEAT";
        public static final String CONNECTION_SUCCESS = "CONNECTION_SUCCESS";
    }

    /**
     * 创建新通知消息
     */
    public static <T> WebSocketMessage<T> newNotification(Long userId, T data) {
        return WebSocketMessage.<T>builder()
                .type(MessageType.NEW_NOTIFICATION)
                .userId(userId)
                .data(data)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 创建未读数量更新消息
     */
    public static WebSocketMessage<Long> unreadCountUpdate(Long userId, Long unreadCount) {
        return WebSocketMessage.<Long>builder()
                .type(MessageType.UNREAD_COUNT_UPDATE)
                .userId(userId)
                .data(unreadCount)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 创建心跳消息
     */
    public static WebSocketMessage<String> heartbeat(Long userId) {
        return WebSocketMessage.<String>builder()
                .type(MessageType.HEARTBEAT)
                .userId(userId)
                .data("ping")
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 创建连接成功消息
     */
    public static WebSocketMessage<String> connectionSuccess(Long userId) {
        return WebSocketMessage.<String>builder()
                .type(MessageType.CONNECTION_SUCCESS)
                .userId(userId)
                .data("WebSocket连接成功")
                .timestamp(System.currentTimeMillis())
                .build();
    }
}