package com.cc.booktalk.common.redis;
import org.springframework.data.redis.core.RedisTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Component
public class RedisCacheUtils {

    @Resource
    private RedisTemplate<String, Object> customObjectRedisTemplate;


    /**
     * 获取缓存数据
     * @param redisKey
     * @param limit
     * @param clazz
     * @return
     * @param <T>
     */
    public <T> List<T> getCacheDataLimit(String redisKey, int limit, Class<T> clazz) {
        try {
            Object cachedData = customObjectRedisTemplate.opsForValue().get(redisKey);

            if (cachedData instanceof List) {
                @SuppressWarnings("unchecked")
                List<T> cacheData = (List<T>) cachedData;

                if (!cacheData.isEmpty()) {
                    return cacheData.stream()
                            .limit(limit)
                            .collect(Collectors.toList());
                }
            }

            log.debug("缓存数据为空或不存在: key={}", redisKey);
            return Collections.emptyList();

        } catch (Exception e) {
            log.error("从Redis获取缓存数据失败: key={}, limit={}, error={}",redisKey, limit, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * 设置缓存数据
     */
    public <T> void setCacheData(String redisKey, List<T> data, long timeout, TimeUnit timeUnit) {
        try {
            if (data == null) {
                data = Collections.emptyList();
            }
            customObjectRedisTemplate.opsForValue().set(redisKey, data, timeout, timeUnit);
            log.debug("设置缓存数据成功: key={}, size={}", redisKey, data.size());
        } catch (Exception e) {
            log.error("设置缓存数据失败: key={}, error={}", redisKey, e.getMessage(), e);
        }
    }







}
