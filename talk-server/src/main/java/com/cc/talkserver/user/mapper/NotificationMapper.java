package com.cc.talkserver.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cc.talkpojo.dto.NotificationQueryDTO;
import com.cc.talkpojo.entity.Notification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 通知Mapper接口
 *
 * @author cc
 * @since 2025-10-12
 */
@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {

    /**
     * 根据用户ID查询通知列表
     * @param userId 用户ID
     * @param queryDTO 查询条件
     * @return 通知列表
     */
    List<Notification> selectByUserId(@Param("userId") Long userId,
                                     @Param("query") NotificationQueryDTO queryDTO);

    /**
     * 统计用户未读通知数量
     * @param userId 用户ID
     * @return 未读数量
     */
    @Select("SELECT COUNT(*) FROM notification WHERE user_id = #{userId} AND is_read = FALSE AND is_deleted = FALSE")
    Long countUnreadByUserId(Long userId);

    /**
     * 批量标记为已读
     * @param userId 用户ID
     * @param notificationIds 通知ID列表
     * @return 更新行数
     */
    int batchMarkAsRead(@Param("userId") Long userId,
                       @Param("notificationIds") List<Long> notificationIds);

    /**
     * 批量删除通知
     * @param userId 用户ID
     * @param notificationIds 通知ID列表
     * @return 删除行数
     */
    int batchDelete(@Param("userId") Long userId,
                   @Param("notificationIds") List<Long> notificationIds);

    /**
     * 清空已读通知
     * @param userId 用户ID
     * @return 删除行数
     */
    int clearReadNotifications(Long userId);
}