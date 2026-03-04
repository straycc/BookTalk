package com.cc.booktalk.interfaces.schedule;


import com.cc.booktalk.application.user.service.recommendation.UserInterestCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
@Slf4j
public class UserInterestCacheJob {
    @Resource
    private UserInterestCacheService userInterestCacheService;

    @Scheduled(cron = "0 0 2 * * ?")
    public void updateUserTopInterestsCache() {
        log.info("开始执行用户兴趣缓存定时任务");
        userInterestCacheService.updateUserTopInterestsCache();
    }
}
