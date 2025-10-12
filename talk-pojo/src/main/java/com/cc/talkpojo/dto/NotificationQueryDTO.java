package com.cc.talkpojo.dto;

import lombok.Data;

/**
 * 通知查询DTO
 *
 * @author cc
 * @since 2025-10-12
 */
@Data
public class NotificationQueryDTO {

    /**
     * 页码
     */
    private Integer page = 1;

    /**
     * 每页数量
     */
    private Integer size = 20;

    /**
     * 是否已读筛选
     */
    private Boolean isRead;

    /**
     * 通知类型筛选
     */
    private String type;
}