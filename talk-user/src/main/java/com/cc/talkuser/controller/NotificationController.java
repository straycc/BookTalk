package com.cc.talkuser.controller;

import com.cc.talkcommon.result.Result;
import com.cc.talkpojo.dto.NotificationQueryDTO;
import com.cc.talkpojo.result.PageResult;
import com.cc.talkpojo.vo.NotificationVO;
import com.cc.talkserver.user.service.NotificationService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 通知控制器
 *
 * @author cc
 * @since 2025-10-12
 */
@Slf4j
@Api(tags = "通知管理")
@RestController
@RequestMapping("/user/notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    /**
     * 获取通知列表
     */
    @GetMapping
    @ApiOperation("获取通知列表")
    public Result<PageResult<NotificationVO>> getNotificationList(NotificationQueryDTO queryDTO) {
        PageResult<NotificationVO> result = notificationService.getNotificationList(queryDTO);
        return Result.success(result);
    }

    /**
     * 获取未读通知数量
     */
    @GetMapping("/unread/count")
    @ApiOperation("获取未读通知数量")
    public Result<Long> getUnreadCount() {
        Long count = notificationService.getUnreadCount();
        return Result.success(count);
    }

    /**
     * 标记通知为已读
     */
    @PutMapping("/{id}/read")
    @ApiOperation("标记通知为已读")
    public Result<Void> markAsRead(
            @ApiParam("通知ID") @PathVariable Long id) {
        notificationService.markAsRead(id);
        return Result.success();
    }

    /**
     * 批量标记为已读
     */
    @PutMapping("/batch/read")
    @ApiOperation("批量标记为已读")
    public Result<Void> batchMarkAsRead(
            @ApiParam("通知ID列表") @RequestBody List<Long> notificationIds) {
        notificationService.batchMarkAsRead(notificationIds);
        return Result.success();
    }

    /**
     * 删除通知
     */
    @DeleteMapping("/{id}")
    @ApiOperation("删除通知")
    public Result<Void> deleteNotification(
            @ApiParam("通知ID") @PathVariable Long id) {
        notificationService.deleteNotification(id);
        return Result.success();
    }

    /**
     * 批量删除通知
     */
    @DeleteMapping("/batch")
    @ApiOperation("批量删除通知")
    public Result<Void> batchDeleteNotifications(
            @ApiParam("通知ID列表") @RequestBody List<Long> notificationIds) {
        notificationService.batchDeleteNotifications(notificationIds);
        return Result.success();
    }

    /**
     * 清空已读通知
     */
    @DeleteMapping("/read")
    @ApiOperation("清空已读通知")
    public Result<Void> clearReadNotifications() {
        notificationService.clearReadNotifications();
        return Result.success();
    }
}