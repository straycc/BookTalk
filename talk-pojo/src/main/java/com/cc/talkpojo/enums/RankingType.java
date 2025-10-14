package com.cc.talkpojo.enums;

/**
 * 榜单类型枚举
 *
 * @author cc
 * @since 2025-10-13
 */
public enum RankingType {

    HOT_REVIEWS("hot_reviews", "热门书评"),
    BOOK_RATING("book_rating", "图书评分榜"),
    HOT_DISCUSSION("hot_discussion", "热门讨论"),
    NEW_BOOKS("new_books", "新书榜");

    private final String code;
    private final String desc;

    RankingType(String code, String desc) {
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