package com.cc.talkpojo.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BookReviewVO {

    private Long bookReviewId;
    private Long bookId;
    private Integer type;
    private String title;
    private String content;
    private Integer score;
    private Integer likeCount;

    // 作者信息
    private Long userId;
    private String nickName;
    private String avatarUrl;

    // 时间信息
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    // 评论状态
    private Integer status;

    // 当前用户是否点赞
    private Boolean likedByCurrentUser;
}

