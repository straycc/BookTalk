package com.cc.booktalk.application.user.service.recommendation;

import com.cc.booktalk.entity.entity.recommendation.UserInterestTag;
import com.cc.booktalk.infrastructure.persistence.user.mapper.recommendation.UserInterestTagMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.Duration;
import java.util.List;

@Slf4j
@Service
public class UserInterestCacheService {

    @Resource
    private UserInterestTagMapper userInterestTagMapper;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * Redis缓存键前缀
     */
    private static final String USER_TOP_INTERESTS_KEY = "user:top_interests:";

    /**
     * 定时更新用户Top5兴趣缓存
     * 每天凌晨2点执行
     */
    public void updateUserTopInterestsCache() {
        try {
            log.info("开始定时更新用户Top5兴趣缓存");

            // 获取所有有兴趣标签的用户
            List<Long> userIds = getAllUsersWithInterests();
            log.info("获取到用户数量: {}", userIds.size());

            int successCount = 0;
            for (Long userId : userIds) {
                try {
                    // 获取用户Top5兴趣标签并存入Redis
                    List<UserInterestTag> topInterests = userInterestTagMapper.getTopUserInterests(userId, 5);

                    if (!topInterests.isEmpty()) {
                        String cacheKey = USER_TOP_INTERESTS_KEY + userId;
                        redisTemplate.opsForValue().set(cacheKey, topInterests, Duration.ofHours(24));
                        log.debug("用户兴趣缓存更新成功: userId={}, 标签数量={}", userId, topInterests.size());
                    }
                    successCount++;
                } catch (Exception e) {
                    log.error("更新用户缓存失败: userId={}", userId, e);
                }
            }

            log.info("用户兴趣缓存更新完成: 成功数量={}", successCount);

        } catch (Exception e) {
            log.error("定时更新用户兴趣缓存失败", e);
        }
    }

    /**
     * 获取所有有兴趣标签的用户
     */
    private List<Long> getAllUsersWithInterests() {
        return userInterestTagMapper.getAllUsersWithInterests();
    }
}
