package com.cc.talkserver.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

/**
 * WebSocket配置类
 *
 * @author cc
 * @since 2025-10-12
 */
@Configuration
public class WebSocketConfig {

    /**
     * 支持使用@ServerEndpoint注解的WebSocket
     */
    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }
}