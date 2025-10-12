package com.cc.talkserver.listener;

import com.cc.talkcommon.event.NotificationEvent;
import com.cc.talkserver.user.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * RabbitMQ通知消息消费者
 *
 * @author cc
 * @since 2025-10-12
 */
@Slf4j
@Component
public class NotificationConsumer {

    @Autowired
    private NotificationService notificationService;

    /**
     * 消费通知消息
     * @param event 通知事件
     */
    @RabbitListener(queues = "notification.queue")
    @RabbitHandler
    public void handleNotification(NotificationEvent event) {
        try {
            log.info("开始处理通知消息: 类型={}, 接收者={}, 发送者={}",
                    event.getType(), event.getUserId(), event.getSenderId());

            // 创建通知记录
            notificationService.createNotification(event);

            // TODO: WebSocket实时推送
            // webSocketService.sendNotification(event);

            log.info("通知消息处理完成: 类型={}, 接收者={}", event.getType(), event.getUserId());
        } catch (Exception e) {
            log.error("处理通知消息失败", e);
            // 这里可以实现重试逻辑或者将消息发送到死信队列
            throw e; // 重新抛出异常，触发重试机制
        }
    }
}