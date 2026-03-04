package com.cc.booktalk.domain.notification;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
public class NotificationDomainService {

    public String resolveTypeDesc(String type) {
        switch (type) {
            case "LIKE":
                return "点赞";
            case "COMMENT":
                return "评论";
            case "REPLY":
                return "回复";
            case "FOLLOW":
                return "关注";
            case "SYSTEM":
                return "系统";
            default:
                return "未知";
        }
    }

    public String resolveTimeDesc(LocalDateTime createTime) {
        if (createTime == null) {
            return "未知";
        }
        long minutes = ChronoUnit.MINUTES.between(createTime, LocalDateTime.now());

        if (minutes < 1) {
            return "刚刚";
        } else if (minutes < 60) {
            return minutes + "分钟前";
        } else if (minutes < 1440) {
            return (minutes / 60) + "小时前";
        } else {
            return (minutes / 1440) + "天前";
        }
    }
}
