package com.cc.booktalk.application.user.service.notification;

import com.cc.booktalk.common.event.NotificationEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Slf4j
@Service
public class NotificationMessageService {

    @Resource
    private NotificationService notificationService;

    public void processNotificationMessage(NotificationEvent event) {
        log.info("开始处理通知消息: 类型={}, 接收者={}, 发送者={}",
                event.getType(), event.getUserId(), event.getSenderId());

        notificationService.createNotification(event);

        log.info("通知消息处理完成: 类型={}, 接收者={}", event.getType(), event.getUserId());
    }
}
