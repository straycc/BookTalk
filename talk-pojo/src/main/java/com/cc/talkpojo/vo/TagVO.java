package com.cc.talkpojo.vo;

import lombok.Data;

import java.time.LocalDateTime;


@Data
public class TagVO {
    /**
     * 标签ID
     */
    private Long id;

    /**
     * 标签创建者id
     */
    private Long creatorId;

    /**
     * 分类ID
     */
    private Long categoryId;

    /**
     * 分类名称
     */
    private String categoryName;

    /**
     * 标签名称
     */
    private String name;


    /**
     * 标签描述
     */
    private String description;

    /**
     * 标签使用次数
     */
    private Long usageCount;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
