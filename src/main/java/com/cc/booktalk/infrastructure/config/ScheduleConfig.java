package com.cc.booktalk.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * 定时任务配置
 *
 * @author cc
 * @since 2025-10-13
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "app.scheduling.enabled", havingValue = "true")
public class ScheduleConfig {
    // 启用Spring的定时任务功能
}
