package com.cc.talkpojo.entity;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TagES {

    private Long id;

    /**
     * 标签创建者id
     */
    private Long creatorId;

    /**
     * 分类
     */
    private Long categoryId;

    /**
     * 标签名称
     */
    private String name;

    /**
     * 标签描述
     */
    private String description;

}
