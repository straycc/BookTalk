package com.cc.talkserver.listener;

import com.cc.talkcommon.event.NotificationEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Spring Event监听器 (监听并转发到RabbitMQ)
 *
 * @author cc
 * @since 2025-10-12
 */
@Slf4j
@Component
public class NotificationEventListener {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * 监听通知事件并转发到RabbitMQ
     * @param event 通知事件
     */
    @EventListener
    @Async
    public void handleNotificationEvent(NotificationEvent event) {
        try {
            log.info("接收到Spring Event事件: 类型={}, 接收者={}", event.getType(), event.getUserId());

            // 转发到RabbitMQ队列
            rabbitTemplate.convertAndSend(
                "notification.exchange",
                "notification.key",
                event,
                message -> {
                    // 设置消息属性
                    message.getMessageProperties().setContentType("application/json");
                    message.getMessageProperties().setExpiration("86400000"); // 24小时过期
                    return message;
                }
            );

            log.info("通知事件已转发到RabbitMQ: 类型={}, 接收者={}", event.getType(), event.getUserId());
        } catch (Exception e) {
            log.error("转发通知事件到RabbitMQ失败", e);
            // 这里可以实现降级策略，比如直接创建通知记录
        }
    }
}