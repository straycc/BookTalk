package com.cc.booktalk.interfaces.mq.consumer;

import com.cc.booktalk.common.event.NotificationEvent;
import com.cc.booktalk.application.user.service.notification.NotificationMessageService;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * RabbitMQ通知消息消费者
 *
 * @author cc
 * @since 2025-10-12
 */
@Component
public class NotificationConsumer {

    @Resource
    private NotificationMessageService notificationMessageService;

    /**
     * 消费通知消息
     * @param event 通知事件
     */
    @RabbitListener(queues = "notification.queue")
    @RabbitHandler
    public void handleNotification(NotificationEvent event) {
        notificationMessageService.processNotificationMessage(event);
    }
}
