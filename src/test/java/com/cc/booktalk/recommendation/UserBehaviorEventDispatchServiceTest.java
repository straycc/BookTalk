package com.cc.booktalk.recommendation;

import com.cc.booktalk.application.user.service.recommendation.behavior.UserBehaviorEventDispatchService;
import com.cc.booktalk.application.user.service.recommendation.behavior.UserBehaviorMessageService;
import com.cc.booktalk.common.event.behavior.UserBehaviorEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserBehaviorEventDispatchServiceTest {

    @InjectMocks private UserBehaviorEventDispatchService dispatchService;
    @Mock private RabbitTemplate rabbitTemplate;
    @Mock private UserBehaviorMessageService userBehaviorMessageService;

    @AfterEach
    void clearSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void dispatchesOnlyAfterTransactionCommit() {
        TransactionSynchronizationManager.initSynchronization();
        UserBehaviorEvent event = UserBehaviorEvent.builder()
                .userId(1L).targetId(10L).targetType("BOOK").behaviorType("BOOK_VIEW").build();

        dispatchService.publish(event);

        verify(rabbitTemplate, never()).convertAndSend(
                eq("user.behavior.exchange"), eq("user.behavior.routing.key"), eq(event));
        for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
            synchronization.afterCommit();
        }
        verify(rabbitTemplate).convertAndSend(
                eq("user.behavior.exchange"), eq("user.behavior.routing.key"), eq(event));
    }
}
