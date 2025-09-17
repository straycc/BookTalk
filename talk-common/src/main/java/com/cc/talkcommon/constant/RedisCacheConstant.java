package com.cc.talkcommon.constant;

import jdk.dynalink.beans.StaticClass;

public class RedisCacheConstant {


    public static final String CACHE_BLANK = "";

    public static final String REDISSION_BOOKDETAIL_LOCK_PREFIX = "lock:bookDetail:";

    public static final Integer HOT_BOOKS_COUNT = 100;
    public static final Integer HOT_TAGS_COUNT = 10;
    public static final Integer CACHE_EXPIRE_SECONDS = 30 * 24 * 3600; // 30 天（单位：秒）




    public static final String HOT_BOOKS_KEY_PREFIX = "book:hot:";
    public static final String BOOK_DETAIL_KEY_PREFIX = "book:detail";


    //用户相关
    public static final String USER_INFO_KEY_PREFIX = "user:info:";


    //点赞相关
    public static final String LIKE_USER_PREFIX = "like:user:"; // 用户维度，快速判断是否点赞
    public static final String LIKE_TARGET_PREFIX = "like:target:"; // 目标维度， 快速统计点赞数
    public static final String LIKE_COUNT_PREFIX = "like:count:"; // 点赞计数缓存






}
