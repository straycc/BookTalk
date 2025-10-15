package com.cc.talkserver.config;

import com.cc.talkcommon.Json.JacksonObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;

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

    /**
     * JSON 消息转换器
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        // 使用自定义的 JacksonObjectMapper，支持 LocalDateTime 序列化
        return new Jackson2JsonMessageConverter(new JacksonObjectMapper());
    }

    /**
     * RabbitMQ 监听器容器工厂
     * 确保消费者也使用自定义的消息转换器
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter());
        return factory;
    }

    /**
     * 使用 Spring Boot 自动配置的 ConnectionFactory
     * 不再自定义，避免与 application.yaml 冲突
     */

    /**
     * 通知交换机 - Topic类型
     */
    @Bean
    public TopicExchange notificationExchange() {
        log.info("创建通知交换机: notification.exchange");
        return new TopicExchange("notification.exchange", true, false);// 持久化、手动删除
    }

    /**
     * 通知队列 - 持久化
     */
    @Bean
    public Queue notificationQueue() {
        log.info("创建通知队列: notification.queue");
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
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);

        // 设置 JSON 消息转换器
        rabbitTemplate.setMessageConverter(jsonMessageConverter());

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

    /**
     * RabbitAdmin配置，用于自动声明队列、交换机和绑定关系
     */
    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        log.info("初始化 RabbitAdmin，Connection Factory: {}", connectionFactory.getClass().getSimpleName());

        RabbitAdmin rabbitAdmin = new RabbitAdmin(connectionFactory);
        rabbitAdmin.setAutoStartup(true);
        rabbitAdmin.setIgnoreDeclarationExceptions(true); // 宽松模式，避免启动失败

        // 手动触发声明所有 Bean
        try {
            rabbitAdmin.afterPropertiesSet();
            log.info("RabbitAdmin 初始化完成，开始声明队列和交换机...");

            // 手动声明交换机和队列
            rabbitAdmin.declareExchange(notificationExchange());
            log.info("通知交换机声明成功");

            rabbitAdmin.declareQueue(notificationQueue());
            log.info("通知队列声明成功");

            rabbitAdmin.declareExchange(websocketExchange());
            log.info("WebSocket交换机声明成功");

            rabbitAdmin.declareQueue(websocketNotificationQueue());
            log.info("WebSocket通知队列声明成功");

            // 声明绑定关系
            rabbitAdmin.declareBinding(notificationBinding());
            log.info("通知绑定关系声明成功");

            rabbitAdmin.declareBinding(websocketNotificationBinding());
            log.info("WebSocket绑定关系声明成功");

        } catch (Exception e) {
            log.error("RabbitMQ 队列/交换机声明失败", e);
            throw e;
        }

        return rabbitAdmin;
    }
}