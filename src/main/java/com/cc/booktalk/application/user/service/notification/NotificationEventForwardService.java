package com.cc.booktalk.application.user.service.notification;

import com.cc.booktalk.common.event.NotificationEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Slf4j
@Service
public class NotificationEventForwardService {

    @Resource
    private RabbitTemplate rabbitTemplate;

    public void forwardNotificationEvent(NotificationEvent event) {
        try {
            log.info("接收到Spring Event事件: 类型={}, 接收者={}", event.getType(), event.getUserId());

            rabbitTemplate.convertAndSend(
                    "notification.exchange",
                    "notification.key",
                    event,
                    message -> {
                        message.getMessageProperties().setContentType("application/json");
                        message.getMessageProperties().setExpiration("86400000");
                        return message;
                    }
            );

            log.info("通知事件已转发到RabbitMQ: 类型={}, 接收者={}", event.getType(), event.getUserId());
        } catch (Exception e) {
            log.error("转发通知事件到RabbitMQ失败", e);
        }
    }
}
