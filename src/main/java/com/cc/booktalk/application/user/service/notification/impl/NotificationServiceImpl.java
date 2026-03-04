package com.cc.booktalk.application.user.service.notification.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cc.booktalk.common.context.UserContext;
import com.cc.booktalk.common.event.NotificationEvent;
import com.cc.booktalk.entity.dto.notification.NotificationQueryDTO;
import com.cc.booktalk.entity.entity.notification.Notification;
import com.cc.booktalk.entity.result.PageResult;
import com.cc.booktalk.entity.vo.NotificationVO;
import com.cc.booktalk.application.user.service.notification.NotificationPushService;
import com.cc.booktalk.application.user.service.notification.NotificationService;
import com.cc.booktalk.domain.notification.NotificationDomainService;
import com.cc.booktalk.infrastructure.persistence.user.mapper.notification.NotificationMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.time.LocalDateTime;
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
    private NotificationPushService notificationPushService;

    @Resource
    private NotificationMapper notificationMapper;

    @Resource
    private NotificationDomainService notificationDomainService;

    @Override
    @Transactional
    public void createNotification(NotificationEvent event) {
        log.info("创建通知记录: 类型={}, 接收者={}, 发送者={}",
                event.getType(), event.getUserId(), event.getSenderId());

        // 转换为实体对象
        Notification notification = new Notification();
        BeanUtils.copyProperties(event, notification);
        notification.setIsRead(false);
        notification.setIsDeleted(false);
        notification.setCreateTime(LocalDateTime.now()); // 添加创建时间

        // 保存到数据库
        this.save(notification);

        log.info("通知记录创建成功: ID={}", notification.getId());

        notificationPushService.pushNewNotification(convertToVO(notification));
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
        // 移除手动设置 updateTime，依赖数据库自动更新

        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getId, notificationId)
               .eq(Notification::getUserId, userId);

        if (this.update(notification, wrapper)) {
            // WebSocket推送已读状态更新
            Long unreadCount = notificationMapper.countUnreadByUserId(userId);
            notificationPushService.pushUnreadCountUpdate(userId, unreadCount);
            log.info("通知标记已读成功: ID={}", notificationId);
        }
    }

    @Override
    public void batchMarkAsRead(List<Long> notificationIds) {
        if (notificationIds == null || notificationIds.isEmpty()) {
            log.warn("批量标记已读：通知ID列表为空");
            return;
        }

        Long userId = getCurrentUserId();
        int updateCount = notificationMapper.batchMarkAsRead(userId, notificationIds);

        // WebSocket推送未读数量更新
        Long unreadCount = notificationMapper.countUnreadByUserId(userId);
        notificationPushService.pushUnreadCountUpdate(userId, unreadCount);

        log.info("批量标记已读完成: 数量={}", updateCount);
    }

    @Override
    public void deleteNotification(Long notificationId) {
        Long userId = getCurrentUserId();

        Notification notification = new Notification();
        notification.setId(notificationId);
        notification.setIsDeleted(true);
        // 移除手动设置 updateTime，依赖数据库自动更新

        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getId, notificationId)
               .eq(Notification::getUserId, userId);

        if (this.update(notification, wrapper)) {
            log.info("通知删除成功: ID={}", notificationId);
        }
    }

    @Override
    public void batchDeleteNotifications(List<Long> notificationIds) {
        if (notificationIds == null || notificationIds.isEmpty()) {
            log.warn("批量删除通知：通知ID列表为空");
            return;
        }

        Long userId = getCurrentUserId();
        int deleteCount = notificationMapper.batchDelete(userId, notificationIds);
        log.info("批量删除通知完成: 数量={}", deleteCount);
    }

    @Override
    public void clearReadNotifications() {
        Long userId = getCurrentUserId();
        int clearCount = notificationMapper.clearReadNotifications(userId);

        // WebSocket推送未读数量更新（虽然清空的是已读，但用户可能想知道操作结果）
        Long unreadCount = notificationMapper.countUnreadByUserId(userId);
        notificationPushService.pushUnreadCountUpdate(userId, unreadCount);

        log.info("清空已读通知完成: 数量={}", clearCount);
    }

    /**
     * 转换为VO对象
     */
    private NotificationVO convertToVO(Notification notification) {
        NotificationVO vo = new NotificationVO();
        BeanUtils.copyProperties(notification, vo);

        vo.setTypeDesc(notificationDomainService.resolveTypeDesc(notification.getType()));
        vo.setTimeDesc(notificationDomainService.resolveTimeDesc(notification.getCreateTime()));

        return vo;
    }

    /**
     * 获取当前用户ID
     */
    private Long getCurrentUserId() {
        // TODO: 从SecurityContext或ThreadLocal获取当前登录用户ID
        return UserContext.getUser().getId();
    }
}
