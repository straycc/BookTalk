package com.cc.booktalk.common.utils;

public class UserUtils {

    public static String defaultNickname(Long userId) {
        // 使用用户ID的后4位
        String idSuffix = String.format("%04d", userId % 10000);
        return "书友" + idSuffix;
    }
}
