package com.cc.booktalk.application.user.service.notification;

import com.cc.booktalk.common.websocket.WebSocketMessage;
import com.cc.booktalk.entity.vo.NotificationVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Slf4j
@Service
public class NotificationPushService {

    @Resource
    private RabbitTemplate rabbitTemplate;

    public void pushNewNotification(NotificationVO notificationVO) {
        try {
            WebSocketMessage<NotificationVO> message = WebSocketMessage.newNotification(
                    notificationVO.getUserId(), notificationVO
            );

            rabbitTemplate.convertAndSend(
                    "websocket.exchange",
                    "websocket.notification.key",
                    message,
                    msg -> {
                        msg.getMessageProperties().setContentType("application/json");
                        msg.getMessageProperties().setExpiration("300000");
                        return msg;
                    }
            );

            log.debug("WebSocket通知消息已发送到RabbitMQ: userId={}, notificationId={}",
                    notificationVO.getUserId(), notificationVO.getId());
        } catch (Exception e) {
            log.error("发送WebSocket通知消息到RabbitMQ失败: userId={}", notificationVO.getUserId(), e);
        }
    }

    public void pushUnreadCountUpdate(Long userId, Long unreadCount) {
        try {
            WebSocketMessage<Long> message = WebSocketMessage.unreadCountUpdate(userId, unreadCount);

            rabbitTemplate.convertAndSend(
                    "websocket.exchange",
                    "websocket.notification.key",
                    message,
                    msg -> {
                        msg.getMessageProperties().setContentType("application/json");
                        msg.getMessageProperties().setExpiration("60000");
                        return msg;
                    }
            );

            log.debug("WebSocket未读数量更新消息已发送到RabbitMQ: userId={}, unreadCount={}", userId, unreadCount);
        } catch (Exception e) {
            log.error("发送WebSocket未读数量更新消息到RabbitMQ失败: userId={}", userId, e);
        }
    }
}
