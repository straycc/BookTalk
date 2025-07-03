package com.cc.talkpojo.vo;


import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class PageBookVO {

    /**图书id*/
    private Long id;

    /**图书名*/
    private String title;

    /**原作名称（外文原名）*/
    private String originalTitle;

    /**图书简介*/
    private String description;

    /**图书作者*/
    private String author;

    /**作者国籍*/
    private String authorCountry;

    /**图书出版商*/
    private String publisher;

    /**译者*/
    private String translator;

    /**出品方/品牌方*/
    private String producer;

    /**出版时间*/
    private LocalDate publishDate;

    /**价格*/
    private BigDecimal price;

    /**图书封面*/
    private String coverUrl;

    /**图书分类*/
    private String categoryName;

    /**图书平均评分*/
    private BigDecimal averageScore;

    /**图书收藏人数*/
    private Integer favoriteCount;

}
