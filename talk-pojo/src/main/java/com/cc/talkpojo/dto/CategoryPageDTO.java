package com.cc.talkpojo.dto;


import lombok.Data;

import java.time.LocalDate;

@Data
public class CategoryPageDTO implements PageDTO{

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
     * 起始时间
     */
    private LocalDate createdFrom;


    /**
     * 结束时间
     */
    private LocalDate createdTo;
}
