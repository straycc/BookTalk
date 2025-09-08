package com.cc.talkpojo.dto;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.time.LocalDateTime;


@Data
public class BookReviewDTO {

    /**
     * 评论ID
     */
    private Long bookReviewId;

    /**
     * 图书ID
     */
    private Long bookId;

    /**
     * 评论用户ID
     */
    private Long userId;

    /**
     * 评论类型：0-短评，1-长评
     */
    private Integer type;

    /**
     * 长评标题（短评可为空）
     */
    private String title;

    /**
     * 评论内容（支持 markdown）
     */
    private String content;

    /**
     * 评分（1-10），可选
     */
    private Integer score;

    /**
     * 点赞数
     */
    private Integer likeCount;

    /**
     * 回复数
     */
    private Integer replyCount;

}
