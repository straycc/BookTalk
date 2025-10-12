package com.cc.talkserver.user.service;

import com.cc.talkcommon.event.NotificationEvent;
import com.cc.talkpojo.dto.NotificationQueryDTO;
import com.cc.talkpojo.result.PageResult;
import com.cc.talkpojo.vo.NotificationVO;

/**
 * 通知服务接口
 *
 * @author cc
 * @since 2025-10-12
 */
public interface NotificationService {

    /**
     * 创建通知记录
     * @param event 通知事件
     */
    void createNotification(NotificationEvent event);

    /**
     * 获取通知列表
     * @param queryDTO 查询条件
     * @return 通知列表
     */
    PageResult<NotificationVO> getNotificationList(NotificationQueryDTO queryDTO);

    /**
     * 获取未读通知数量
     * @return 未读数量
     */
    Long getUnreadCount();

    /**
     * 标记通知为已读
     * @param notificationId 通知ID
     */
    void markAsRead(Long notificationId);

    /**
     * 批量标记为已读
     * @param notificationIds 通知ID列表
     */
    void batchMarkAsRead(java.util.List<Long> notificationIds);

    /**
     * 删除通知
     * @param notificationId 通知ID
     */
    void deleteNotification(Long notificationId);

    /**
     * 批量删除通知
     * @param notificationIds 通知ID列表
     */
    void batchDeleteNotifications(java.util.List<Long> notificationIds);

    /**
     * 清空已读通知
     */
    void clearReadNotifications();
}