package com.cc.booktalk.common.utils;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 时间工具类
 *
 * @author cc
 * @since 2025-10-27
 */
public class TimeUtils {

    /**
     * 根据时间周期获取开始时间
     *
     * @param period 时间周期 (daily, weekly, monthly, all_time)
     * @return 开始时间
     */
    public static LocalDateTime getStartTimeByPeriod(String period) {
        LocalDateTime now = LocalDateTime.now();
        switch (period) {
            case "daily":
                return now.minus(1, ChronoUnit.DAYS);
            case "weekly":
                return now.minus(7, ChronoUnit.DAYS);
            case "monthly":
                return now.minus(30, ChronoUnit.DAYS);
            case "all_time":
            default:
                return LocalDateTime.of(2020, 1, 1, 0, 0);
        }
    }

    /**
     * 获取时间描述
     *
     * @param createTime 创建时间
     * @return 时间描述（如：5分钟前、2小时前、3天前）
     */
    public static String getTimeDesc(LocalDateTime createTime) {
        LocalDateTime now = LocalDateTime.now();
        long minutes = ChronoUnit.MINUTES.between(createTime, now);

        if (minutes < 60) {
            return minutes + "分钟前";
        } else if (minutes < 1440) {
            return (minutes / 60) + "小时前";
        } else {
            return (minutes / 1440) + "天前";
        }
    }
}