package com.cc.talkpojo.dto;

import lombok.Data;
import lombok.extern.java.Log;

import java.time.LocalDate;
import java.util.List;

@Data
public class PageBookDTO implements PageDTO{

    /** 页面号*/
    private Integer pageNum;

    /** 页面大小*/
    private Integer pageSize;

    /** 书名*/
    private String isbn;

    /** 书名*/
    private String title;

    /** 出版时间*/
    private LocalDate publishDate;

    /** 作者*/
    private String author;

    /** 分类筛选*/
    private Long categoryId;

    /** 标签筛选*/
    private Long tagId;

    /** 排序属性*/
    private String sortField;

    /** 排序规则*/
    private String sortOrder;

    /** 起始时间*/
    private LocalDate createdFrom;

    /** 结束时间*/
    private LocalDate createdTo;
}