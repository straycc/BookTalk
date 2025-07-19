package com.cc.talkpojo.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookES {

    private Long id;

    /**
     * ISBN编号
     */
    private String isbn;

    /**
     * 书名
     */
    private String title;

    /**
     * 副标题
     */
    private String subTitle;

    /**
     * 原作名称（外文原名）
     */
    private String originalTitle;

    /**
     * 图书简介
     */
    private String description;

    /**
     * 作者
     */
    private String author;

    /**
     * 作者国籍
     */
    private String authorCountry;

    /**
     * 译者
     */
    private String translator;

    // 出版信息
    /**
     * 出版系列
     */
    private String series;

    /**
     * 出版社
     */
    private String publisher;

    /**
     * 出品方/品牌方
     */
    private String producer;

    /**
     * 出版时间
     */
    private LocalDate publishDate;

    /**
     * 价格
     */
    private BigDecimal price;

    /**
     * 封面图片URL
     */
    private String coverUrl;

    /**
     * 分类ID（单分类）
     */
    private Long categoryId;

    /**
     * 分类名称
     */
    private String categoryName;

    /**
     * 平均评分
     */
    private BigDecimal averageScore;

    /**
     * 评分人数
     */
    private Integer scoreCount;

}
