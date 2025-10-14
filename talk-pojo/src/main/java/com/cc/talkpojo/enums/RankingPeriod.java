package com.cc.talkpojo.enums;

/**
 * 榜单时间周期枚举
 *
 * @author cc
 * @since 2025-10-13
 */
public enum RankingPeriod {

    DAILY("daily", "日榜"),
    WEEKLY("weekly", "周榜"),
    MONTHLY("monthly", "月榜"),
    ALL_TIME("all_time", "总榜");

    private final String code;
    private final String desc;

    RankingPeriod(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}