package com.cc.booktalk.interfaces.websocket;

import com.cc.booktalk.common.websocket.WebSocketMessage;
import com.cc.booktalk.application.user.service.notification.WebSocketPushPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Slf4j
@Component
public class WebSocketPushAdapter implements WebSocketPushPort {

    @Resource
    private WebSocketSessionManager sessionManager;

    @Resource
    private ObjectMapper objectMapper;

    @Override
    public void pushToUser(Long userId, WebSocketMessage<?> message) {
        try {
            if (userId == null || message == null) {
                log.warn("WebSocket推送参数为空: userId={}, message={}", userId, message);
                return;
            }

            String jsonMessage = objectMapper.writeValueAsString(message);
            sessionManager.sendMessageToUser(userId, jsonMessage);
        } catch (Exception e) {
            log.error("WebSocket消息推送失败: userId={}", userId, e);
        }
    }
}
