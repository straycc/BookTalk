package com.cc.talkserver.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ配置类
 *
 * @author cc
 * @since 2025-10-12
 */
@Slf4j
@Configuration
@EnableRabbit
public class RabbitMQConfig {

    @Autowired
    private ConnectionFactory connectionFactory;

    /**
     * 通知交换机 - Topic类型
     */
    @Bean
    public TopicExchange notificationExchange() {
        return new TopicExchange("notification.exchange", true, false);// 持久化、手动删除
    }

    /**
     * 通知队列 - 持久化
     */
    @Bean
    public Queue notificationQueue() {
        return QueueBuilder.durable("notification.queue")
                .ttl(24 * 60 * 60 * 1000) // 24小时TTL
                .maxLength(10000) // 最大队列长度
                .build();
    }

    /**
     * 死信交换机
     */
    @Bean
    public TopicExchange notificationDlxExchange() {
        return new TopicExchange("notification.dlx.exchange", true, false);
    }

    /**
     * 死信队列
     */
    @Bean
    public Queue notificationDlxQueue() {
        return QueueBuilder.durable("notification.dlx.queue").build();
    }

    /**
     * 死信队列绑定
     */
    @Bean
    public Binding notificationDlxBinding() {
        return BindingBuilder.bind(notificationDlxQueue())
                .to(notificationDlxExchange())
                .with("notification.dlx.key");
    }

    /**
     * 通知队列绑定到交换机
     */
    @Bean
    public Binding notificationBinding() {
        return BindingBuilder.bind(notificationQueue())
                .to(notificationExchange())
                .with("notification.key");
    }

    /**
     * WebSocket交换机
     */
    @Bean
    public TopicExchange websocketExchange() {
        return new TopicExchange("websocket.exchange", true, false);
    }

    /**
     * WebSocket通知队列
     */
    @Bean
    public Queue websocketNotificationQueue() {
        return QueueBuilder.durable("websocket.notification.queue")
                .ttl(5 * 60 * 1000) // 5分钟TTL
                .maxLength(5000) // 最大队列长度
                .build();
    }

    /**
     * WebSocket通知队列绑定
     */
    @Bean
    public Binding websocketNotificationBinding() {
        return BindingBuilder.bind(websocketNotificationQueue())
                .to(websocketExchange())
                .with("websocket.notification.key");
    }

    /**
     * RabbitTemplate 配置
     */
    @Bean
    public RabbitTemplate rabbitTemplate() {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);

        // 开启发送确认
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            if (ack) {
                log.info("消息发送成功: correlationData={}", correlationData);
            } else {
                log.error("消息发送失败: correlationData={}, cause={}", correlationData, cause);
            }
        });

        // 开启返回确认
        rabbitTemplate.setReturnsCallback(returned -> {
            log.error("消息返回: message={}, replyCode={}, replyText={}, exchange={}, routingKey={}",
                    returned.getMessage(), returned.getReplyCode(), returned.getReplyText(),
                    returned.getExchange(), returned.getRoutingKey());
        });

        // 开启Mandatory，当消息无法路由时返回给生产者
        rabbitTemplate.setMandatory(true);

        return rabbitTemplate;
    }
}