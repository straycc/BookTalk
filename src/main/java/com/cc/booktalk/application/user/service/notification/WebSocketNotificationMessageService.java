package com.cc.booktalk.application.user.service.notification;

import com.cc.booktalk.common.websocket.WebSocketMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Slf4j
@Service
public class WebSocketNotificationMessageService {

    @Resource
    private WebSocketPushPort webSocketPushPort;

    public void processWebSocketMessage(WebSocketMessage<?> message) {
        try {
            log.debug("接收到WebSocket通知消息: type={}, userId={}, data={}",
                    message.getType(), message.getUserId(), message.getData());

            switch (message.getType()) {
                case WebSocketMessage.MessageType.NEW_NOTIFICATION:
                    pushNewNotification(message);
                    break;
                case WebSocketMessage.MessageType.UNREAD_COUNT_UPDATE:
                    pushUnreadCountUpdate(message);
                    break;
                default:
                    log.warn("未知的WebSocket消息类型: {}", message.getType());
            }
        } catch (Exception e) {
            log.error("处理WebSocket通知消息失败: message={}", message, e);
        }
    }

    private void pushNewNotification(WebSocketMessage<?> message) {
        Long userId = message.getUserId();
        log.debug("推送新通知到WebSocket: userId={}", userId);
        webSocketPushPort.pushToUser(userId, message);
        log.info("WebSocket新通知推送成功: userId={}", userId);
    }

    private void pushUnreadCountUpdate(WebSocketMessage<?> message) {
        Long userId = message.getUserId();
        log.debug("推送未读数量更新到WebSocket: userId={}, count={}", userId, message.getData());
        webSocketPushPort.pushToUser(userId, message);
        log.info("WebSocket未读数量更新推送成功: userId={}", userId);
    }
}
