package com.cc.talkpojo.enums;


import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ReviewType {
    SHORT(0, "短评"),
    LONG(1, "长评");

    private final int code;
    private final String desc;


    // 校验方法
    public static boolean isValid(int code) {
        for (ReviewType type : ReviewType.values()) {
            if (type.getCode() == code) {
                return true;
            }
        }
        return false;
    }
}
