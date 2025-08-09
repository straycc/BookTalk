package com.cc.talkpojo.dto;


import lombok.Data;

@Data
public class TagDTO {

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
}
