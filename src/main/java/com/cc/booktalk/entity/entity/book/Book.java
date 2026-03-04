package com.cc.booktalk.entity.entity.book;

import java.math.BigDecimal;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.io.Serializable;

import lombok.*;
import lombok.experimental.Accessors;

/**
 * <p>
 * 图书主表
 * </p>
 *
 * @author cc
 * @since 2025-06-30
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("book")
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public class Book implements Serializable {
    private static final long serialVersionUID = 1L;


    /**
     * 图书ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
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
     * 页数
     */
    private Integer pageCount;

    /**
     * 装帧类型
     */
    private String bindingType;


    /**
     * 分类ID（单分类）
     */
    private Long categoryId;

    /**
     * 平均评分
     */
    @TableField("average_score")
    private Double averageScore;

    /**
     * 五星评价占比
     */
    @TableField("stars5_top")
    private BigDecimal stars5Top;

    /**
     * 四星评价占比
     */
    @TableField("stars4_top")
    private BigDecimal stars4Top;

    /**
     * 三星评价占比
     */
    @TableField("stars3_top")
    private BigDecimal stars3Top;

    /**
     * 二星评价占比
     */
    @TableField("stars2_top")
    private BigDecimal stars2Top;

    /**
     * 一星评价占比
     */
    @TableField("stars1_top")
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
     * 热度值
     */
    private double hotScore;


    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 热度更新时间
     */
    private LocalDateTime hotScoreUpdateTime;
}
