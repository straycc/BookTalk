package com.cc.talkpojo.dto;

import lombok.Data;

import java.time.LocalDateTime;


@Data
public class BookTagDTO {

    /**
     * 标签ID
     */
    private Long id;

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
