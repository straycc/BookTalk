package com.cc.booktalk.application.user.service.recommendation;

import com.cc.booktalk.common.event.behavior.UserBehaviorEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Slf4j
@Service
public class UserBehaviorEventDispatchService {

    private static final String USER_BEHAVIOR_EXCHANGE = "user.behavior.exchange";
    private static final String USER_BEHAVIOR_ROUTING_KEY = "user.behavior.routing.key";

    @Resource
    private RabbitTemplate rabbitTemplate;

    @Resource
    private UserBehaviorMessageService userBehaviorMessageService;

    public void publish(UserBehaviorEvent behaviorEvent) {
        if (behaviorEvent == null) {
            return;
        }
        try {
            rabbitTemplate.convertAndSend(
                    USER_BEHAVIOR_EXCHANGE,
                    USER_BEHAVIOR_ROUTING_KEY,
                    behaviorEvent
            );
        } catch (Exception e) {
            log.error("发送用户行为消息到MQ失败: userId={}, behaviorType={}",
                    behaviorEvent.getUserId(), behaviorEvent.getBehaviorType(), e);
            try {
                userBehaviorMessageService.processUserBehavior(behaviorEvent);
                log.warn("已降级为本地处理用户行为: userId={}, behaviorType={}, targetId={}",
                        behaviorEvent.getUserId(), behaviorEvent.getBehaviorType(), behaviorEvent.getTargetId());
            } catch (Exception ex) {
                log.error("本地处理用户行为也失败: userId={}, behaviorType={}",
                        behaviorEvent.getUserId(), behaviorEvent.getBehaviorType(), ex);
            }
        }
    }
}
