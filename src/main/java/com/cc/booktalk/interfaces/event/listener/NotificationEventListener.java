package com.cc.booktalk.interfaces.event.listener;

import com.cc.booktalk.common.event.NotificationEvent;
import com.cc.booktalk.application.user.service.notification.NotificationEventForwardService;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * Spring Event监听器 (监听并转发到RabbitMQ)
 *
 * @author cc
 * @since 2025-10-12
 */
@Component
public class NotificationEventListener {

    @Resource
    private NotificationEventForwardService notificationEventForwardService;

    /**
     * 监听通知事件并转发到RabbitMQ
     * @param event 通知事件
     */
    @EventListener
    @Async
    public void handleNotificationEvent(NotificationEvent event) {
        notificationEventForwardService.forwardNotificationEvent(event);
    }
}
