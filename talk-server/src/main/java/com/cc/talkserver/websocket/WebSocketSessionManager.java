package com.cc.talkserver.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.websocket.Session;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * WebSocket会话管理器
 *
 * @author cc
 * @since 2025-10-12
 */
@Slf4j
@Component
public class WebSocketSessionManager {

    /**
     * 用户ID与WebSocket会话的映射
     * Key: userId, Value: Session
     */
    private static final ConcurrentHashMap<Long, Session> USER_SESSIONS = new ConcurrentHashMap<>();

    /**
     * 存储所有在线用户ID
     */
    private static final CopyOnWriteArraySet<Long> ONLINE_USERS = new CopyOnWriteArraySet<>();

    /**
     * 添加用户会话
     * @param userId 用户ID
     * @param session WebSocket会话
     */
    public void addSession(Long userId, Session session) {
        USER_SESSIONS.put(userId, session);
        ONLINE_USERS.add(userId);
        log.info("用户 {} 建立WebSocket连接，当前在线人数: {}", userId, ONLINE_USERS.size());
    }

    /**
     * 移除用户会话
     * @param userId 用户ID
     */
    public void removeSession(Long userId) {
        Session session = USER_SESSIONS.remove(userId);
        ONLINE_USERS.remove(userId);

        if (session != null && session.isOpen()) {
            try {
                session.close();
            } catch (IOException e) {
                log.error("关闭WebSocket会话失败: userId={}", userId, e);
            }
        }

        log.info("用户 {} 断开WebSocket连接，当前在线人数: {}", userId, ONLINE_USERS.size());
    }

    /**
     * 获取用户会话
     * @param userId 用户ID
     * @return WebSocket会话
     */
    public Session getSession(Long userId) {
        return USER_SESSIONS.get(userId);
    }

    /**
     * 检查用户是否在线
     * @param userId 用户ID
     * @return 是否在线
     */
    public boolean isUserOnline(Long userId) {
        return ONLINE_USERS.contains(userId);
    }

    /**
     * 获取在线用户数量
     * @return 在线用户数量
     */
    public int getOnlineUserCount() {
        return ONLINE_USERS.size();
    }

    /**
     * 向指定用户发送消息
     * @param userId 用户ID
     * @param message 消息内容
     * @return 是否发送成功
     */
    public boolean sendMessageToUser(Long userId, String message) {
        Session session = USER_SESSIONS.get(userId);
        if (session != null && session.isOpen()) {
            try {
                synchronized (session) {
                    session.getBasicRemote().sendText(message);
                }
                log.debug("向用户 {} 发送WebSocket消息成功: {}", userId, message);
                return true;
            } catch (IOException e) {
                log.error("向用户 {} 发送WebSocket消息失败: {}", userId, message, e);
                // 发送失败，移除会话
                removeSession(userId);
                return false;
            }
        }
        log.debug("用户 {} 不在线或WebSocket会话已关闭", userId);
        return false;
    }

    /**
     * 向所有在线用户广播消息
     * @param message 消息内容
     */
    public void broadcastToAll(String message) {
        int successCount = 0;
        int failCount = 0;

        for (Long userId : ONLINE_USERS) {
            if (sendMessageToUser(userId, message)) {
                successCount++;
            } else {
                failCount++;
            }
        }

        log.info("WebSocket广播完成，成功: {}, 失败: {}, 总在线: {}",
                successCount, failCount, ONLINE_USERS.size());
    }

    /**
     * 获取所有在线用户ID
     * @return 在线用户ID集合
     */
    public CopyOnWriteArraySet<Long> getOnlineUsers() {
        return new CopyOnWriteArraySet<>(ONLINE_USERS);
    }

    /**
     * 清理无效会话
     */
    public void cleanupInvalidSessions() {
        int removedCount = 0;
        for (Long userId : ONLINE_USERS) {
            Session session = USER_SESSIONS.get(userId);
            if (session == null || !session.isOpen()) {
                removeSession(userId);
                removedCount++;
            }
        }

        if (removedCount > 0) {
            log.info("清理无效WebSocket会话: {} 个", removedCount);
        }
    }
}