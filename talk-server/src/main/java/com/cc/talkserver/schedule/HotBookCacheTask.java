package com.cc.talkserver.schedule;

import com.cc.talkserver.admin.service.BookAdminService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

//@Component
//public class HotBookCacheTask {
//
//    @Resource
//    private BookAdminService bookAdminService;
//
//    @Scheduled(cron = "0 0 2 1 * ?") // 每月1号凌晨2点执行
//    public void refreshMonthlyHotBooksCache() {
//        bookAdminService.refreshHotBooksCache();
//    }
//}