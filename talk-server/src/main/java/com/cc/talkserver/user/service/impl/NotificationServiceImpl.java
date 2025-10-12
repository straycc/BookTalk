package com.cc.talkserver.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cc.talkcommon.context.UserContext;
import com.cc.talkcommon.event.NotificationEvent;
import com.cc.talkcommon.websocket.WebSocketMessage;
import com.cc.talkpojo.dto.NotificationQueryDTO;
import com.cc.talkpojo.entity.Notification;
import com.cc.talkpojo.result.PageResult;
import com.cc.talkpojo.vo.NotificationVO;
import com.cc.talkserver.user.mapper.NotificationMapper;
import com.cc.talkserver.user.service.NotificationService;
import com.cc.talkserver.websocket.NotificationWebSocketHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 通知服务实现类
 *
 * @author cc
 * @since 2025-10-12
 */
@Slf4j
@Service
public class NotificationServiceImpl extends ServiceImpl<NotificationMapper, Notification> implements NotificationService {

    @Resource
    private RabbitTemplate rabbitTemplate;

    @Resource
    private NotificationMapper notificationMapper;

    @Override
    public void createNotification(NotificationEvent event) {
        log.info("创建通知记录: 类型={}, 接收者={}, 发送者={}",
                event.getType(), event.getUserId(), event.getSenderId());

        // 转换为实体对象
        Notification notification = new Notification();
        BeanUtils.copyProperties(event, notification);
        notification.setIsRead(false);
        notification.setIsDeleted(false);

        // 保存到数据库
        this.save(notification);

        // 转换为VO并推送WebSocket消息
        NotificationVO notificationVO = convertToVO(notification);
        pushNotificationViaWebSocket(notificationVO);

        log.info("通知记录创建成功: ID={}", notification.getId());
    }

    @Override
    public PageResult<NotificationVO> getNotificationList(NotificationQueryDTO queryDTO) {
        Long userId = getCurrentUserId();

        // 构建查询条件
        LambdaQueryWrapper<Notification> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Notification::getUserId, userId)
                   .eq(Notification::getIsDeleted, false)
                   .orderByDesc(Notification::getCreateTime);

        // 状态筛选
        if (queryDTO.getIsRead() != null) {
            queryWrapper.eq(Notification::getIsRead, queryDTO.getIsRead());
        }

        // 类型筛选
        if (StringUtils.hasText(queryDTO.getType())) {
            queryWrapper.eq(Notification::getType, queryDTO.getType());
        }

        // 分页查询
        com.github.pagehelper.PageHelper.startPage(queryDTO.getPage(), queryDTO.getSize());
        List<Notification> notifications = this.list(queryWrapper);

        com.github.pagehelper.PageInfo<Notification> pageInfo =
                new com.github.pagehelper.PageInfo<>(notifications);

        // 转换为VO
        List<NotificationVO> voList = notifications.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return new PageResult<>(pageInfo.getTotal(), voList);
    }

    @Override
    public Long getUnreadCount() {
        Long userId = getCurrentUserId();
        return notificationMapper.countUnreadByUserId(userId);
    }

    @Override
    public void markAsRead(Long notificationId) {
        Long userId = getCurrentUserId();

        Notification notification = new Notification();
        notification.setId(notificationId);
        notification.setIsRead(true);

        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getId, notificationId)
               .eq(Notification::getUserId, userId);

        if (this.update(notification, wrapper)) {
            // WebSocket推送已读状态更新
            pushUnreadCountUpdate(userId);
            log.info("通知标记已读成功: ID={}", notificationId);
        }
    }

    @Override
    public void batchMarkAsRead(List<Long> notificationIds) {
        Long userId = getCurrentUserId();
        int updateCount = notificationMapper.batchMarkAsRead(userId, notificationIds);

        // WebSocket推送未读数量更新
        pushUnreadCountUpdate(userId);

        log.info("批量标记已读完成: 数量={}", updateCount);
    }

    @Override
    public void deleteNotification(Long notificationId) {
        Long userId = getCurrentUserId();

        Notification notification = new Notification();
        notification.setId(notificationId);
        notification.setIsDeleted(true);

        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getId, notificationId)
               .eq(Notification::getUserId, userId);

        if (this.update(notification, wrapper)) {
            log.info("通知删除成功: ID={}", notificationId);
        }
    }

    @Override
    public void batchDeleteNotifications(List<Long> notificationIds) {
        Long userId = getCurrentUserId();
        int deleteCount = notificationMapper.batchDelete(userId, notificationIds);
        log.info("批量删除通知完成: 数量={}", deleteCount);
    }

    @Override
    public void clearReadNotifications() {
        Long userId = getCurrentUserId();
        int clearCount = notificationMapper.clearReadNotifications(userId);

        // WebSocket推送未读数量更新（虽然清空的是已读，但用户可能想知道操作结果）
        pushUnreadCountUpdate(userId);

        log.info("清空已读通知完成: 数量={}", clearCount);
    }

    /**
     * 转换为VO对象
     */
    private NotificationVO convertToVO(Notification notification) {
        NotificationVO vo = new NotificationVO();
        BeanUtils.copyProperties(notification, vo);

        // 设置类型描述
        vo.setTypeDesc(getTypeDesc(notification.getType()));

        // 设置时间描述
        vo.setTimeDesc(getTimeDesc(notification.getCreateTime()));

        return vo;
    }

    /**
     * 获取类型描述
     */
    private String getTypeDesc(String type) {
        switch (type) {
            case "LIKE":
                return "点赞";
            case "COMMENT":
                return "评论";
            case "REPLY":
                return "回复";
            case "FOLLOW":
                return "关注";
            case "SYSTEM":
                return "系统";
            default:
                return "未知";
        }
    }

    /**
     * 获取时间描述
     */
    private String getTimeDesc(LocalDateTime createTime) {
        LocalDateTime now = LocalDateTime.now();
        long minutes = ChronoUnit.MINUTES.between(createTime, now);

        if (minutes < 1) {
            return "刚刚";
        } else if (minutes < 60) {
            return minutes + "分钟前";
        } else if (minutes < 1440) {
            return (minutes / 60) + "小时前";
        } else {
            return (minutes / 1440) + "天前";
        }
    }

    /**
     * 获取当前用户ID
     */
    private Long getCurrentUserId() {
        // TODO: 从SecurityContext或ThreadLocal获取当前登录用户ID
        return UserContext.getUser().getId();
    }

    /**
     * 通过WebSocket推送新通知
     * @param notificationVO 通知VO
     */
    private void pushNotificationViaWebSocket(NotificationVO notificationVO) {
        try {
            WebSocketMessage<NotificationVO> message = WebSocketMessage.newNotification(
                    notificationVO.getUserId(), notificationVO);

            // 使用WebSocket处理器发送消息
            NotificationWebSocketHandler.sendMessageToUser(notificationVO.getUserId(), message);

            log.debug("WebSocket推送新通知成功: userId={}, notificationId={}",
                    notificationVO.getUserId(), notificationVO.getId());
        } catch (Exception e) {
            log.error("WebSocket推送新通知失败: userId={}", notificationVO.getUserId(), e);
        }
    }

    /**
     * 通过WebSocket推送未读数量更新
     * @param userId 用户ID
     */
    private void pushUnreadCountUpdate(Long userId) {
        try {
            Long unreadCount = notificationMapper.countUnreadByUserId(userId);
            WebSocketMessage<Long> message = WebSocketMessage.unreadCountUpdate(userId, unreadCount);

            // 使用WebSocket处理器发送消息
            NotificationWebSocketHandler.sendMessageToUser(userId, message);

            log.debug("WebSocket推送未读数量更新成功: userId={}, unreadCount={}", userId, unreadCount);
        } catch (Exception e) {
            log.error("WebSocket推送未读数量更新失败: userId={}", userId, e);
        }
    }
}