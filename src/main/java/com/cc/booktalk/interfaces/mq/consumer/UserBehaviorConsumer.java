package com.cc.booktalk.interfaces.mq.consumer;

import com.cc.booktalk.common.event.behavior.UserBehaviorEvent;
import com.cc.booktalk.application.user.service.recommendation.UserBehaviorMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import javax.annotation.Resource;

/**
 * 用户行为消息监听器
 * 监听用户行为消息队列，处理用户行为记录
 *
 * @author cc
 * @since 2024-01-15
 */
@Slf4j
@Component
public class UserBehaviorConsumer {

    @Resource
    private UserBehaviorMessageService userBehaviorMessageService;


    /**
     * 监听用户行为消息队列
     *
     * @param behaviorDTO 用户行为数据
     */
    @RabbitListener(queues = "user.behavior.queue")
    public void processUserBehavior(UserBehaviorEvent behaviorDTO) {
        try {
            log.debug("接收到用户行为消息: userId={}, behaviorType={}, targetId={}",
                     behaviorDTO.getUserId(), behaviorDTO.getBehaviorType(), behaviorDTO.getTargetId());
            userBehaviorMessageService.processUserBehavior(behaviorDTO);

            log.debug("用户行为消息处理完成: userId={}, behaviorType={}, targetId={}",
                     behaviorDTO.getUserId(), behaviorDTO.getBehaviorType(), behaviorDTO.getTargetId());

        } catch (Exception e) {
            log.error("处理用户行为消息失败: userId={}, behaviorType={}",
                     behaviorDTO.getUserId(), behaviorDTO.getBehaviorType(), e);
            // 根据实际需求，可以添加重试逻辑或死信队列处理
        }
    }


}
