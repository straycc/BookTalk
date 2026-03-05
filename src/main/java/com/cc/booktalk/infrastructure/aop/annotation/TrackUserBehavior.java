package com.cc.booktalk.infrastructure.aop.annotation;

import java.lang.annotation.*;

/**
 * 用户行为记录注解
 * 用于标记需要记录用户行为的方法
 *
 * @author cc
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TrackUserBehavior {

    /**
     * 行为类型
     */
    String behaviorType();

    /**
     * 目标类型
     */
    String targetType() default "";

    /**
     * 用户ID参数名
     */
    String userIdParam() default "userId";

    /**
     * 目标ID参数名
     */
    String targetIdParam() default "id";

    /**
     * 行为分数
     */
    double behaviorScore() default 1.0;

    /**
     * 额外数据表达式
     * 支持SpEL表达式，如 #{paramName} 或 #{paramName.field}
     */
    String extraData() default "";

    /**
     * 是否异步处理
     */
    boolean async() default true;
}