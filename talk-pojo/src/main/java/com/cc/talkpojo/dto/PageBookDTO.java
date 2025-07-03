package com.cc.talkpojo.dto;

import lombok.Data;

import java.util.List;

@Data
public class PageBookDTO {

    /** 页面号*/
    private Integer pageNum;

    /** 页面大小*/
    private Integer pageSize;

    // 搜索条件
    /** 书名*/
    private String isbn;

    /** 书名*/
    private String title;

    /** 作者*/
    private String author;

    /** 分类筛选*/
    private Long categoryId;

    /** 标签筛选*/
    private List<Long> tagIds;


    private String sortField;


    private String sortOrder;
}