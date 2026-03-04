package com.cc.booktalk.application.user.service.recommendation;

import com.cc.booktalk.common.constant.RedisCacheConstant;
import com.cc.booktalk.entity.vo.PersonalizedRecVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.Duration;
import java.util.List;

@Slf4j
@Service
public class PersonalizedRecService {

    @Resource
    private RecommendationService recommendationService;

    @Resource
    private UserBehaviorService userBehaviorService;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 每天凌晨3点更新所有活跃用户的个性化推荐
     */
    public void updateActivateUserRecommendations() {
        log.info("开始定时更新所有用户个性化推荐");
        // 获取所有活跃用户（近30天有行为记录的用户）
        List<Long> activeUsers = getActiveUsers();
        log.info("获取到活跃用户数量: {}", activeUsers.size());

        int successCount = 0;
        int failCount = 0;

        for (Long userId : activeUsers) {
            try {
                // 为用户生成个性化推荐
                List<PersonalizedRecVO> recommendations = recommendationService
                        .getPersonalizedRecommendations(userId, 10);

                if (!recommendations.isEmpty()) {
                    String cacheKey = RedisCacheConstant.RECOMMENDATIONS_PREFIX + ":" + userId;
                    // 缓存24小时
                    redisTemplate.opsForValue().set(cacheKey, recommendations, Duration.ofHours(24));
                    log.debug("用户推荐更新成功: userId={}, 推荐数量={}", userId, recommendations.size());
                }
                successCount++;
            } catch (Exception e) {
                log.error("更新用户推荐失败: userId={}", userId, e);
                failCount++;
            }
        }
        log.info("个性化推荐更新完成: 成功={}, 失败={}", successCount, failCount);
    }

    /**
     * 每6小时更新热门推荐缓存
     */
    public void updateHotRecommendations() {
        log.info("开始更新热门推荐缓存");

        // 更新热门推荐（用于冷启动和个性化推荐补充）
        List<PersonalizedRecVO> hotRecommendations = recommendationService.getHotRecommendations(50);

        String cacheKey = RedisCacheConstant.RECOMMENDATIONS_HOT;
        redisTemplate.opsForValue().set(cacheKey, hotRecommendations, Duration.ofHours(6));

        log.info("热门推荐更新完成: 推荐数量={}", hotRecommendations.size());
    }

    /**
     * 获取活跃用户列表（近30天有行为的用户）
     */
    private List<Long> getActiveUsers() {
        // 获取近30天内至少有5次行为的活跃用户
        return userBehaviorService.getActiveUsers(30, 5);
    }

}
