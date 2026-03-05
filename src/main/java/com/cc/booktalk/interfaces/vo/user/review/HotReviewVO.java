package com.cc.booktalk.interfaces.vo.user.review;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 热门书评视图对象
 *
 * @author cc
 * @since 2025-10-13
 */
@Data
public class HotReviewVO implements Serializable {

    /**
     * 书评ID
     */
    private Long reviewId;

    /**
     * 书评标题
     */
    private String title;

    /**
     * 书评内容（截取）
     */
    private String content;

    /**
     * 书籍ID
     */
    private Long bookId;

    /**
     * 书籍名称
     */
    private String bookName;

    /**
     * 书籍封面
     */
    private String bookCover;


    /**
     * 作者
     */
    private String author;

    /**
     * 作者头像
     */
    private String authorAvatar;

    /**
     * 点赞数
     */
    private Long likeCount;

    /**
     * 评论数
     */
    private Long commentCount;

    /**
     * 热度值
     */
    private double hotScore;

    /**
     * 排名
     */
    private Integer rank;

    /**
     * 书评创建时间
     */
    private LocalDateTime createTime;

    /**
     * 时间描述
     */
    private String timeDesc;

    /**
     * 类别
     */
    private Long categoryId;
}