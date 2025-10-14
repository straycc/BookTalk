package com.cc.talkserver.listener;

import com.cc.talkcommon.websocket.WebSocketMessage;
import com.cc.talkserver.websocket.NotificationWebSocketHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * WebSocket通知消息消费者
 * 专门处理需要通过WebSocket推送的消息
 *
 * @author cc
 * @since 2025-10-12
 */
@Slf4j
@Component
public class WebSocketNotificationListener {

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 消费WebSocket通知消息
     * @param messageJson JSON格式的消息
     */
    @RabbitListener(queues = "websocket.notification.queue")
    public void handleWebSocketNotification(@Payload String messageJson) {
        try {
            log.debug("接收到WebSocket通知消息: {}", messageJson);

            // 解析消息
            WebSocketMessage<?> message = objectMapper.readValue(messageJson, WebSocketMessage.class);

            switch (message.getType()) {
                case WebSocketMessage.MessageType.NEW_NOTIFICATION:
                    handleNewNotification(message);
                    break;
                case WebSocketMessage.MessageType.UNREAD_COUNT_UPDATE:
                    handleUnreadCountUpdate(message);
                    break;
                default:
                    log.warn("未知的WebSocket消息类型: {}", message.getType());
            }

        } catch (Exception e) {
            log.error("处理WebSocket通知消息失败: {}", messageJson, e);
        }
    }

    /**
     * 处理新通知消息
     * @param message WebSocket消息
     */
    @SuppressWarnings("unchecked")
    private void handleNewNotification(WebSocketMessage<?> message) {
        try {
            Long userId = message.getUserId();

            log.debug("推送新通知到WebSocket: userId={}", userId);

            // 通过WebSocket发送给用户
            NotificationWebSocketHandler.sendMessageToUser(userId, message);

            log.info("WebSocket新通知推送成功: userId={}", userId);
        } catch (Exception e) {
            log.error("WebSocket新通知推送失败", e);
        }
    }

    /**
     * 处理未读数量更新消息
     * @param message WebSocket消息
     */
    @SuppressWarnings("unchecked")
    private void handleUnreadCountUpdate(WebSocketMessage<?> message) {
        try {
            Long userId = message.getUserId();

            log.debug("推送未读数量更新到WebSocket: userId={}, count={}", userId, message.getData());

            // 通过WebSocket发送给用户
            NotificationWebSocketHandler.sendMessageToUser(userId, message);

            log.info("WebSocket未读数量更新推送成功: userId={}", userId);
        } catch (Exception e) {
            log.error("WebSocket未读数量更新推送失败", e);
        }
    }
}