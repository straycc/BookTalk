package com.cc.talkpojo.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
public class BookListPageDTO implements PageDTO {

    /**
     * 页面号
     */
    private Integer pageNum;

    /**
     * 页面大小
     */
    private Integer pageSize;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 搜索关键字（按书单名称、描述搜索）
     */
    private String keyword;

    /**
     * 排序字段
     */
    private String sortBy = "creat_time";

    /**
     * 是否升序（默认 false：降序）
     */
    private Boolean asc = false;

}

