package com.cc.talkpojo.dto;


import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PageCategoryDTO {

    /**
     * 页面号
     */
    private Integer pageNum;

    /**
     * 页面大小
     */
    private Integer pageSize;

    /**
     * 分类名称
     */
    private String name;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
