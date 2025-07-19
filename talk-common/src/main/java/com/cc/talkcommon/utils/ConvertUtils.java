package com.cc.talkcommon.utils;

import org.springframework.beans.BeanUtils;

import java.util.List;

public class ConvertUtils {

    /**
     * 通用对象属性拷贝（相同属性名自动拷贝）
     *
     * @param source      源对象
     * @param targetClass 目标类
     * @return 转换后的对象
     */
    public static <S, T> T convert(S source, Class<T> targetClass) {
        if (source == null) {
            return null;
        }
        try {
            T target = targetClass.getDeclaredConstructor().newInstance();
            BeanUtils.copyProperties(source, target);
            return target;
        } catch (Exception e) {
            throw new RuntimeException("对象转换失败", e);
        }
    }
}

