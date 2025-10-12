package com.cc.talkserver.websocket;

import com.cc.talkcommon.websocket.WebSocketMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;

/**
 * 通知WebSocket端点
 *
 * @author cc
 * @since 2025-10-12
 */
@Slf4j
@Component
@ServerEndpoint("/ws/notifications/{userId}")
public class NotificationWebSocketHandler {

    private static ObjectMapper objectMapper;

    private static WebSocketSessionManager sessionManager;

    @Autowired
    public void setObjectMapper(ObjectMapper objectMapper) {
        NotificationWebSocketHandler.objectMapper = objectMapper;
    }

    @Autowired
    public void setSessionManager(WebSocketSessionManager sessionManager) {
        NotificationWebSocketHandler.sessionManager = sessionManager;
    }

    /**
     * 连接建立时调用
     */
    @OnOpen
    public void onOpen(Session session, @PathParam("userId") Long userId) {
        try {
            if (userId == null) {
                log.warn("WebSocket连接缺少用户ID，关闭连接: sessionId={}", session.getId());
                session.close();
                return;
            }

            log.info("用户 {} 建立WebSocket连接: sessionId={}", userId, session.getId());

            // 添加到会话管理器
            sessionManager.addSession(userId, session);

            // 发送连接成功消息
            WebSocketMessage<String> message = WebSocketMessage.connectionSuccess(userId);
            sendMessage(session, message);

        } catch (Exception e) {
            log.error("WebSocket连接建立失败: userId={}, sessionId={}", userId, session.getId(), e);
            try {
                session.close();
            } catch (IOException ioException) {
                log.error("关闭WebSocket连接失败", ioException);
            }
        }
    }

    /**
     * 收到消息时调用
     */
    @OnMessage
    public void onMessage(String message, Session session, @PathParam("userId") Long userId) {
        try {
            log.debug("收到WebSocket消息: userId={}, sessionId={}, message={}",
                    userId, session.getId(), message);

            if ("ping".equals(message)) {
                // 心跳响应
                WebSocketMessage<String> pongMessage = WebSocketMessage.heartbeat(userId);
                sendMessage(session, pongMessage);
            }

        } catch (Exception e) {
            log.error("处理WebSocket消息失败: userId={}, sessionId={}",
                    userId, session.getId(), e);
        }
    }

    /**
     * 连接关闭时调用
     */
    @OnClose
    public void onClose(Session session, @PathParam("userId") Long userId) {
        log.info("用户 {} WebSocket连接关闭: sessionId={}", userId, session.getId());

        // 从会话管理器中移除
        if (userId != null) {
            sessionManager.removeSession(userId);
        }
    }

    /**
     * 发生错误时调用
     */
    @OnError
    public void onError(Session session, Throwable error, @PathParam("userId") Long userId) {
        log.error("WebSocket连接错误: userId={}, sessionId={}", userId, session.getId(), error);

        // 从会话管理器中移除
        if (userId != null) {
            sessionManager.removeSession(userId);
        }
    }

    /**
     * 发送消息到指定会话
     * @param session WebSocket会话
     * @param message 消息
     */
    private void sendMessage(Session session, WebSocketMessage<?> message) {
        try {
            if (session.isOpen()) {
                String jsonMessage = objectMapper.writeValueAsString(message);
                session.getBasicRemote().sendText(jsonMessage);
                log.debug("发送WebSocket消息成功: sessionId={}, message={}",
                        session.getId(), jsonMessage);
            }
        } catch (IOException e) {
            log.error("发送WebSocket消息失败: sessionId={}", session.getId(), e);
        }
    }

    /**
     * 静态方法：向指定用户发送消息
     * @param userId 用户ID
     * @param message 消息
     */
    public static void sendMessageToUser(Long userId, WebSocketMessage<?> message) {
        if (sessionManager != null && sessionManager.isUserOnline(userId)) {
            Session session = sessionManager.getSession(userId);
            if (session != null && session.isOpen()) {
                try {
                    String jsonMessage = objectMapper.writeValueAsString(message);
                    session.getBasicRemote().sendText(jsonMessage);
                    log.debug("向用户 {} 发送WebSocket消息成功: {}", userId, jsonMessage);
                } catch (IOException e) {
                    log.error("向用户 {} 发送WebSocket消息失败", userId, e);
                    // 发送失败，移除会话
                    sessionManager.removeSession(userId);
                }
            }
        } else {
            log.debug("用户 {} 不在线或WebSocket会话已关闭", userId);
        }
    }
}