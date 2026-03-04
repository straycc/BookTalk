package com.cc.booktalk.aspect;

import com.cc.booktalk.common.context.UserContext;
import com.cc.booktalk.entity.dto.behavior.UserBehaviorDTO;
import com.cc.booktalk.annotation.TrackUserBehavior;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 用户行为记录切面
 * 拦截带有@TrackUserBehavior注解的方法，自动记录用户行为
 *
 * @author cc
 * @since 2024-01-15
 */
@Slf4j
@Component
public class UserBehaviorAspect {

    @Resource
    private RabbitTemplate rabbitTemplate;

    /**
     * 用户行为MQ配置
     */
    private static final String USER_BEHAVIOR_EXCHANGE = "user.behavior.exchange";
    private static final String USER_BEHAVIOR_ROUTING_KEY = "user.behavior.routing.key";

    /**
     * 环绕通知，记录用户行为
     */
    @Around("@annotation(trackUserBehavior)")
    public Object trackBehavior(ProceedingJoinPoint joinPoint, TrackUserBehavior trackUserBehavior) throws Throwable {

        // 执行原方法
        Object result = joinPoint.proceed();

        try {
            // 构建用户行为DTO
            UserBehaviorDTO behaviorDTO = buildBehaviorDTO(joinPoint, trackUserBehavior, result);

            if (behaviorDTO != null) {
                // 发送到MQ队列
                sendUserBehaviorToMQ(behaviorDTO);
                log.debug("用户行为消息已发送到MQ: userId={}, behaviorType={}, targetId={}",
                         behaviorDTO.getUserId(), behaviorDTO.getBehaviorType(), behaviorDTO.getTargetId());
            }

        } catch (Exception e) {
            log.error("构建用户行为或发送MQ消息失败", e);
            // 不影响原方法的执行
        }

        return result;
    }

    /**
     * 发送用户行为到MQ队列
     */
    private void sendUserBehaviorToMQ(UserBehaviorDTO behaviorDTO) {
        try {
            // 发送消息到RabbitMQ
            rabbitTemplate.convertAndSend(
                USER_BEHAVIOR_EXCHANGE,
                USER_BEHAVIOR_ROUTING_KEY,
                behaviorDTO
            );
        } catch (Exception e) {
            log.error("发送用户行为消息到MQ失败: userId={}, behaviorType={}",
                     behaviorDTO.getUserId(), behaviorDTO.getBehaviorType(), e);
        }
    }

    /**
     * 构建用户行为DTO
     */
    private UserBehaviorDTO buildBehaviorDTO(ProceedingJoinPoint joinPoint,
                                           TrackUserBehavior annotation,
                                           Object result) {
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            Parameter[] parameters = method.getParameters();
            Object[] args = joinPoint.getArgs();

            // 获取请求信息
            HttpServletRequest request = getCurrentRequest();

            // 提取参数值
            Map<String, Object> paramValues = extractParameterValues(parameters, args);

            // 获取用户ID
            Long userId = extractUserId(paramValues, annotation.userIdParam(), request);
            if (userId == null) {
                log.warn("无法获取用户ID，跳过行为记录: method={}", method.getName());
                return null;
            }

            // 获取目标ID
            Long targetId = extractTargetId(paramValues, annotation.targetIdParam(), request);
            if (targetId == null) {
                log.warn("无法获取目标ID，跳过行为记录: method={}", method.getName());
                return null;
            }

            // 获取目标类型
            String targetType = getTargetType(annotation.targetType());

            // 构建基础行为DTO
            UserBehaviorDTO behaviorDTO = UserBehaviorDTO.builder()
                    .userId(userId)
                    .targetId(targetId)
                    .targetType(targetType)
                    .behaviorType(annotation.behaviorType())
                    .behaviorScore(annotation.behaviorScore())
                    .behaviorTime(LocalDateTime.now())
                    .build();

            // 处理额外数据
            String extraData = processExtraData(annotation.extraData(), paramValues, result);
            if (extraData != null && !extraData.trim().isEmpty()) {
                behaviorDTO.setExtraData(extraData);
            }

            return behaviorDTO;

        } catch (Exception e) {
            log.error("构建用户行为DTO失败", e);
            return null;
        }
    }

    /**
     * 提取方法参数值
     */
    private Map<String, Object> extractParameterValues(Parameter[] parameters, Object[] args) {
        Map<String, Object> paramValues = new HashMap<>();

        for (int i = 0; i < parameters.length; i++) {
            String paramName = parameters[i].getName();
            Object paramValue = args[i];
            paramValues.put(paramName, paramValue);
        }

        return paramValues;
    }

    /**
     * 提取用户ID
     * 从ThreadLocal获取当前登录用户信息
     */
    private Long extractUserId(Map<String, Object> paramValues, String userIdParam, HttpServletRequest request) {
        try {
            // 直接从ThreadLocal获取当前用户ID
            Long currentUserId = UserContext.getUser().getId();

            if (currentUserId != null) {
                log.debug("从ThreadLocal获取用户ID: {}", currentUserId);
                return currentUserId;
            }

            log.warn("ThreadLocal中未找到用户信息");

        } catch (Exception e) {
            log.warn("从ThreadLocal获取用户ID失败: {}", e.getMessage());
        }

        // 兜底方案：从方法参数获取
        if (userIdParam != null && !userIdParam.isEmpty()) {
            Object userIdValue = paramValues.get(userIdParam);
            if (userIdValue != null) {
                log.debug("从方法参数获取用户ID: {}", userIdValue);
                return convertToLong(userIdValue);
            }
        }

        log.warn("无法获取用户ID，跳过行为记录");
        return null;
    }

    
    /**
     * 提取目标ID
     */
    private Long extractTargetId(Map<String, Object> paramValues, String targetIdParam, HttpServletRequest request) {
        // 1. 从方法参数中获取
        Object targetIdValue = paramValues.get(targetIdParam);
        if (targetIdValue != null) {
            return convertToLong(targetIdValue);
        }

        // 2. 从URL路径中获取
        if (request != null) {
            String pathInfo = request.getRequestURI();
            // 尝试从路径中提取ID，如 /api/books/123
            String[] pathParts = pathInfo.split("/");
            for (String part : pathParts) {
                if (part.matches("\\d+")) {
                    try {
                        return Long.parseLong(part);
                    } catch (NumberFormatException e) {
                        // 忽略，继续尝试
                    }
                }
            }
        }

        return null;
    }

    /**
     * 获取目标类型
     * 直接从注解获取，不进行推断
     */
    private String getTargetType(String targetType) {
        // 如果注解中指定了targetType，直接返回
        if (!targetType.isEmpty()) {
            return targetType;
        }

        // 如果没有指定，返回默认值
        return "BOOK";
    }

    /**
     * 处理额外数据
     */
    private String processExtraData(String extraDataExpr, Map<String, Object> paramValues, Object result) {
        if (extraDataExpr.isEmpty()) {
            return null;
        }

        // 简单的表达式处理，支持 #{paramName} 格式
        if (extraDataExpr.startsWith("#{") && extraDataExpr.endsWith("}")) {
            String paramName = extraDataExpr.substring(2, extraDataExpr.length() - 1);
            Object value = getNestedValue(paramValues, paramName);
            if (value != null) {
                return value.toString();
            }
        } else {
            // 直接返回字符串
            return extraDataExpr;
        }

        return null;
    }

    /**
     * 获取嵌套属性值
     */
    private Object getNestedValue(Map<String, Object> paramValues, String expression) {
        String[] parts = expression.split("\\.");
        Object value = paramValues.get(parts[0]);

        for (int i = 1; i < parts.length && value != null; i++) {
            try {
                // 使用反射获取嵌套属性
                java.lang.reflect.Field field = value.getClass().getDeclaredField(parts[i]);
                field.setAccessible(true);
                value = field.get(value);
            } catch (Exception e) {
                log.warn("无法获取嵌套属性: {}", expression);
                return null;
            }
        }

        return value;
    }

    /**
     * 转换为Long类型
     */
    private Long convertToLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Long) {
            return (Long) value;
        }
        if (value instanceof Integer) {
            return ((Integer) value).longValue();
        }
        if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * 获取当前请求
     */
    private HttpServletRequest getCurrentRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attributes != null ? attributes.getRequest() : null;
        } catch (Exception e) {
            return null;
        }
    }

}