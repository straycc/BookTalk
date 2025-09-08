package com.cc.talkpojo.enums;


public enum ReviewType {
    SHORT(0, "短评"),
    LONG(1, "长评");

    private final int code;
    private final String desc;

    ReviewType(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

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
