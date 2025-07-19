package com.cc.talkserver.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@Slf4j
public class RedisConfiguration {

    // 用于缓存字符串（如热门图书 ID 列表）
    @Bean("customStringRedisTemplate")
    public RedisTemplate<String, String> stringRedisTemplate(RedisConnectionFactory factory) {
        log.info("创建 stringRedisTemplate");
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        StringRedisSerializer serializer = new StringRedisSerializer();
        template.setKeySerializer(serializer);
        template.setValueSerializer(serializer);
        template.setHashKeySerializer(serializer);
        template.setHashValueSerializer(serializer);

        template.afterPropertiesSet();
        return template;
    }

    // 用于缓存对象（如 Book 对象）
    @Bean("customObjectRedisTemplate")
    public RedisTemplate<String, Object> objectRedisTemplate(RedisConnectionFactory factory) {
        log.info("创建 objectRedisTemplate");

        // 建模板实例，并接入 SpringBoot 自动配置好的连接工厂
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        // 两种序列化器
        //    ─ key / hashKey  ➜ StringRedisSerializer：UTF‑8 字符串 → 二进制
        //    ─ value / hashValue ➜ GenericJackson2JsonRedisSerializer：任意对象 → JSON → 二进制
        StringRedisSerializer keySerializer = new StringRedisSerializer();
        GenericJackson2JsonRedisSerializer valueSerializer = new GenericJackson2JsonRedisSerializer();

        // 为四类数据（key、value、hashKey、hashValue）分别指定序列化策略
        template.setKeySerializer(keySerializer);
        template.setValueSerializer(valueSerializer);
        template.setHashKeySerializer(keySerializer);
        template.setHashValueSerializer(valueSerializer);
        // 触发模板内置的属性检查和初始化逻辑
        template.afterPropertiesSet();

        // 返回给 Spring 容器
        return template;
    }
}
