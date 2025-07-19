package com.cc.talkpojo.dto.admin;


import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class AdminBookDTO implements Serializable {

    private static final long serialVersionUID = 1L;


    /**
     * 图书ID
     */
    private Long id;

    /**
     * ISBN编号
     */
    private String isbn;

    //  图书核心元数据
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

    //  作者/译者信息
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

    //  物理特征
    /**
     * 封面图片URL
     */
    private String coverUrl;

    /**
     * 页数
     */
    private Integer pageCount;

    /**
     * 装帧类型
     */
    private String bindingType;

    //  分类与统计
    /**
     * 分类ID（单分类）
     */
    private Long categoryId;

    /**
     * 平均评分
     */
    private BigDecimal averageScore;

    /**
     * 五星评价占比
     */
    private BigDecimal stars5Top;

    /**
     * 四星评价占比
     */
    private BigDecimal stars4Top;

    /**
     * 三星评价占比
     */
    private BigDecimal stars3Top;

    /**
     * 二星评价占比
     */
    private BigDecimal stars2Top;

    /**
     * 一星评价占比
     */
    private BigDecimal stars1Top;


    /**
     * 评分人数
     */
    private Integer scoreCount;

    /**
     * 收藏人数
     */
    private Integer favoriteCount;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;


}
