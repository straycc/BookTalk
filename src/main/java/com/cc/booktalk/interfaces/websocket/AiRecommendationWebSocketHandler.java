package com.cc.booktalk.interfaces.websocket;

import cn.hutool.jwt.JWT;
import com.cc.booktalk.application.user.service.ai.AiRecommendationService;
import com.cc.booktalk.common.jwt.JwtUtil;
import com.cc.booktalk.interfaces.config.SpringEndpointConfigurator;
import com.cc.booktalk.interfaces.dto.user.UserDTO;
import com.cc.booktalk.interfaces.dto.user.ai.AiRecommendationMessageDTO;
import com.cc.booktalk.interfaces.vo.user.ai.AiRecommendationResponseVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.websocket.CloseReason;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * AI 推荐对话 WebSocket 端点。
 */
@Slf4j
@Component
@ServerEndpoint(value = "/ws/ai/recommend/{userId}", configurator = SpringEndpointConfigurator.class)
public class AiRecommendationWebSocketHandler {

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private AiRecommendationService aiRecommendationService;

    @OnOpen
    public void onOpen(Session session, @PathParam("userId") Long userId) {
        try {
            if (userId == null) {
                closeWithReason(session, CloseReason.CloseCodes.CANNOT_ACCEPT, "missing userId");
                return;
            }
            String token = extractToken(session);
            UserDTO tokenUser = parseUserFromToken(token);
            if (tokenUser == null || tokenUser.getId() == null || !userId.equals(tokenUser.getId())) {
                closeWithReason(session, CloseReason.CloseCodes.VIOLATED_POLICY, "invalid token");
                return;
            }

            send(session, AiRecommendationResponseVO.builder()
                    .type("AI_CONNECTION_SUCCESS")
                    .answer("AI 推荐对话连接成功，你可以直接告诉我想看什么样的书。")
                    .followUpSuggestions(List.of("推荐更治愈一点的书", "想看像哈利波特一样的作品", "给我推荐适合入门的心理学书"))
                    .build());
        } catch (Exception e) {
            log.error("AI WebSocket 建连失败: userId={}, sessionId={}", userId, session.getId(), e);
            closeWithReason(session, CloseReason.CloseCodes.UNEXPECTED_CONDITION, "init failed");
        }
    }

    @OnMessage
    public void onMessage(String message, Session session, @PathParam("userId") Long userId) {
        try {
            if ("ping".equalsIgnoreCase(message)) {
                send(session, AiRecommendationResponseVO.builder()
                        .type("PING")
                        .answer("pong")
                        .build());
                return;
            }

            AiRecommendationMessageDTO request = objectMapper.readValue(message, AiRecommendationMessageDTO.class);
            if (request.getType() == null || request.getType().isBlank()) {
                sendError(session, "消息类型不能为空");
                return;
            }

            AiRecommendationResponseVO response;
            if ("RESET_SESSION".equalsIgnoreCase(request.getType())) {
                response = aiRecommendationService.reset(userId, request.getSessionId());
            } else {
                response = aiRecommendationService.ask(userId, request.getSessionId(), request.getContent(), request.getType());
            }
            send(session, response);
        } catch (Exception e) {
            log.error("AI WebSocket 处理失败: userId={}, sessionId={}", userId, session.getId(), e);
            sendError(session, e.getMessage());
        }
    }

    @OnClose
    public void onClose(Session session, @PathParam("userId") Long userId) {
        log.info("AI WebSocket 关闭: userId={}, sessionId={}", userId, session.getId());
    }

    @OnError
    public void onError(Session session, Throwable error, @PathParam("userId") Long userId) {
        log.error("AI WebSocket 异常: userId={}, sessionId={}", userId, session.getId(), error);
    }

    private void sendError(Session session, String message) {
        send(session, AiRecommendationResponseVO.builder()
                .type("AI_RECOMMENDATION_ERROR")
                .error(message)
                .answer("AI 推荐处理失败，请稍后再试。")
                .build());
    }

    private void send(Session session, AiRecommendationResponseVO message) {
        try {
            if (session.isOpen()) {
                session.getBasicRemote().sendText(objectMapper.writeValueAsString(message));
            }
        } catch (IOException e) {
            log.error("AI WebSocket 发送失败: sessionId={}", session.getId(), e);
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
        if (token != null && token.startsWith("Bearer ")) {
            return token.substring(7);
        }
        return token;
    }

    private UserDTO parseUserFromToken(String token) {
        try {
            JWT jwt = JwtUtil.verifyToken(token);
            return JwtUtil.parseUserDTO(jwt);
        } catch (Exception e) {
            log.warn("AI WebSocket token 校验失败: {}", e.getMessage());
            return null;
        }
    }

    private void closeWithReason(Session session, CloseReason.CloseCode code, String reason) {
        try {
            session.close(new CloseReason(code, reason));
        } catch (IOException e) {
            log.error("关闭 AI WebSocket 失败: sessionId={}", session.getId(), e);
        }
    }
}
