package com.cc.talkpojo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookEsDTO {

    /**图书id*/
    private Long id;

    /**ISBN编号*/
    private String isbn;

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
    private Long categoryId;
}
