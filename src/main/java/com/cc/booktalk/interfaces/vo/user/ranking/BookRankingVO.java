package com.cc.booktalk.interfaces.vo.user.ranking;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 书籍榜单视图对象
 *
 * @author cc
 * @since 2025-10-13
 */
@Data
public class BookRankingVO implements Serializable {

    /**
     * 书籍ID
     */
    private Long bookId;

    /**
     * 书籍名称
     */
    private String bookTitle;

    /**
     * 书籍封面
     */
    private String bookCover;
    /**
     * 作者
     */
    private String author;


    /**
     * 分类
     */
    private Long categoryId;

    /**
     * 平均评分
     */
    private Double avgRating;

    /**
     * 评价人数
     */
    private Integer ratingCount;

    /**
     * 书评数量
     */
    private Integer reviewCount;

    /**
     * 阅读量/浏览量
     */
    private Long readCount;

    /**
     * 排名
     */
    private Integer rank;

    /**
     * 上次排名（用于显示排名变化）
     */
    private Integer lastRank;

    /**
     * 排名变化（1=上升，-1=下降，0=不变）
     */
    private Integer rankChange;

    /**
     * 发布时间
     */
    private LocalDateTime publishTime;


    /**
     * 热度分数
     */
    private Double hotScore;

    /**
     * 榜单类型
     */
    private String rankingType;

    /**
     * 榜单周期
     */
    private String rankingPeriod;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;


    /**
     *推荐理由
     */
    private Double rankingScore;

    /**
     * 推荐理由
     */
    private String reason;
}