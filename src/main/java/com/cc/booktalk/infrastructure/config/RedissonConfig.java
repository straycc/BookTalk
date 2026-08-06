package com.cc.booktalk.infrastructure.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;

@Configuration
public class RedissonConfig {

    @Bean
    public RedissonClient redissonClient(RedisProperties redisProperties,
                                         @Value("${app.redis.redisson.netty-threads:2}") int nettyThreads) {
        Config config = new Config();
        config.setNettyThreads(nettyThreads);
        String address = "redis://" + redisProperties.getHost() + ":" + redisProperties.getPort();
        config.useSingleServer()
                .setAddress(address)
                .setDatabase(redisProperties.getDatabase())
                .setConnectionMinimumIdleSize(1)
                .setConnectionPoolSize(10);
        if (redisProperties.getPassword() != null && !redisProperties.getPassword().isBlank()) {
            config.useSingleServer().setPassword(redisProperties.getPassword());
        }
        return Redisson.create(config);
    }
}
