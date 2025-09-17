package com.cc.talkpojo.dto;

import lombok.Data;

@Data
public class PageReviewDTO implements PageDTO {

    /** 页面号 */
    private Integer pageNum = 1;

    /** 页面大小 */
    private Integer pageSize = 10;

    /** 评论类型（0-短评，1-长评，可选） */
    private Integer type;

    /** 图书ID（必传） */
    private Long bookId;

    /** 用户ID（可选：查某个用户的评论） */
    private Long userId;


    private String sortField; // createTime | score | likeCount

    private String sortOrder; // asc | desc
}
