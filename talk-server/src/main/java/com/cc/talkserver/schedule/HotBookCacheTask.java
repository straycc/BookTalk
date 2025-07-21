package com.cc.talkserver.schedule;

import com.cc.talkserver.admin.service.BookAdminService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class HotBookCacheTask {

    @Resource
    private BookAdminService bookAdminService;

    /**
     * 刷新热门图书缓存
     */
    @Scheduled(cron = "0 0 2 1 * ?") // 每月1号凌晨2点执行
//    @Scheduled(cron = "0 */1 * * * ?") // 每分钟执行一次（仅用于测试）

    public void refreshMonthlyHotBooksCache() {

        bookAdminService.refreshHotBooksCache();
    }
}