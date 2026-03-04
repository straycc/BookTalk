package com.cc.booktalk.interfaces.mq.consumer;

import com.cc.booktalk.common.websocket.WebSocketMessage;
import com.cc.booktalk.application.user.service.notification.WebSocketNotificationMessageService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * WebSocket通知消息消费者
 * 专门处理需要通过WebSocket推送的消息
 *
 * @author cc
 * @since 2025-10-12
 */
@Component
public class WebSocketNotificationConsumer {

    @Resource
    private WebSocketNotificationMessageService webSocketNotificationMessageService;

    /**
     * 消费WebSocket通知消息
     * @param message WebSocket消息对象
     */
    @RabbitListener(queues = "websocket.notification.queue", containerFactory = "rabbitListenerContainerFactory")
    public void handleWebSocketNotification(WebSocketMessage<?> message) {
        webSocketNotificationMessageService.processWebSocketMessage(message);
    }
}
