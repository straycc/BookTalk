package com.cc.talkpojo.dto;
import lombok.Data;

import java.time.LocalDateTime;


@Data
public class CategoryDTO {
    /**
     * 分类ID
     */
    private Long id;

    /**
     * 分类名称
     */
    private String name;

    /**
     * 分类描述
     */
    private String description;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

}
