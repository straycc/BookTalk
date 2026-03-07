package com.cc.booktalk.interfaces.websocket;

import cn.hutool.jwt.JWT;
import com.cc.booktalk.common.jwt.JwtUtil;
import com.cc.booktalk.common.websocket.WebSocketMessage;
import com.cc.booktalk.interfaces.dto.user.UserDTO;
import com.cc.booktalk.interfaces.config.SpringEndpointConfigurator;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.annotation.Resource;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 通知WebSocket端点
 *
 * @author cc
 * @since 2025-10-12
 */
@Slf4j
@Component
@ServerEndpoint(value = "/ws/notifications/{userId}", configurator = SpringEndpointConfigurator.class)
public class NotificationWebSocketHandler {

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private WebSocketSessionManager sessionManager;

    /**
     * 连接建立时调用
     */
    @OnOpen
    public void onOpen(Session session, @PathParam("userId") Long userId) {
        try {
            if (userId == null) {
                log.warn("WebSocket连接缺少用户ID，关闭连接: sessionId={}", session.getId());
                closeWithReason(session, CloseReason.CloseCodes.CANNOT_ACCEPT, "missing userId");
                return;
            }

            String token = extractToken(session);
            if (token == null || token.isBlank()) {
                log.warn("WebSocket连接缺少token，关闭连接: sessionId={}, userId={}", session.getId(), userId);
                closeWithReason(session, CloseReason.CloseCodes.VIOLATED_POLICY, "missing token");
                return;
            }

            UserDTO tokenUser = parseUserFromToken(token);
            if (tokenUser == null || tokenUser.getId() == null || !userId.equals(tokenUser.getId())) {
                log.warn("WebSocket token校验失败，关闭连接: sessionId={}, pathUserId={}, tokenUserId={}",
                        session.getId(), userId, tokenUser == null ? null : tokenUser.getId());
                closeWithReason(session, CloseReason.CloseCodes.VIOLATED_POLICY, "invalid token");
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

    private String extractToken(Session session) {
        Map<String, List<String>> params = session.getRequestParameterMap();
        if (params == null) {
            return null;
        }
        List<String> tokenValues = params.get("token");
        if (tokenValues == null || tokenValues.isEmpty()) {
            return null;
        }
        String token = tokenValues.get(0);
        if (token == null) {
            return null;
        }
        if (token.startsWith("Bearer ")) {
            return token.substring(7);
        }
        return token;
    }

    private UserDTO parseUserFromToken(String token) {
        try {
            JWT jwt = JwtUtil.verifyToken(token);
            return JwtUtil.parseUserDTO(jwt);
        } catch (Exception e) {
            log.warn("WebSocket token解析失败: {}", e.getMessage());
            return null;
        }
    }

    private void closeWithReason(Session session, CloseReason.CloseCode code, String reason) {
        try {
            session.close(new CloseReason(code, reason));
        } catch (IOException e) {
            log.error("关闭WebSocket连接失败: sessionId={}", session.getId(), e);
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
}
