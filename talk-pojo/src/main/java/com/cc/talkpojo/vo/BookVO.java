package com.cc.talkpojo.vo;


import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class BookVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**图书id*/
    private Long id;

    /**图书名*/
    private String title;

    /**ISBN编号*/
    private String isbn;

    /**原作名称（外文原名）*/
    private String originalTitle;

    /**图书简介*/
    private String description;

    /**图书作者*/
    private String author;

    /**作者国籍*/
    private String authorCountry;

    /**图书出社*/
    private String publisher;


    /**出品方*/
    private String producer;

    /**译者*/
    private String translator;


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
