package com.cc.talkcommon.constant;

import jdk.dynalink.beans.StaticClass;

public class RedisCacheConstant {


    public static final Integer HOT_BOOKS_COUNT = 100;
    public static final Integer HOT_TAGS_COUNT = 10;
    public static final Integer CACHE_EXPIRE_SECONDS = 30 * 24 * 3600; // 30 天（单位：秒）




    public static final String HOT_BOOKS_KEY_PREFIX = "monthly_hot_books:";
    public static final String BOOK_DETAIL_KEY_PREFIX = "book:";




}
