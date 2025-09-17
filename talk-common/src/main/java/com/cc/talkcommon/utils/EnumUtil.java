package com.cc.talkcommon.utils;

public class EnumUtil {
    /**
     * 根据枚举的 code 字段匹配，返回对应枚举
     * @param enumClass 枚举类型的 class
     * @param code      传入的 code 值
     * @param <E>       枚举类型
     * @return          匹配到的枚举实例
     */
    public static <E extends Enum<E>> E fromCode(Class<E> enumClass, String code) {
        for (E e : enumClass.getEnumConstants()) {
            try {
                // 反射获取枚举的 getCode 方法
                String value = (String) enumClass.getMethod("getCode").invoke(e);
                if (value.equals(code)) {
                    return e;
                }
            } catch (Exception ex) {
                throw new RuntimeException("枚举未定义 getCode 方法: " + enumClass.getName(), ex);
            }
        }
        throw new IllegalArgumentException("非法的枚举值: " + code);
    }
}

