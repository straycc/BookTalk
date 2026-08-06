package com.cc.booktalk.interfaces.vo.user.review;

import com.cc.booktalk.interfaces.vo.user.tag.TagVO;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

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

    // 当前用户是否点赞
    private Boolean likedByCurrentUser;

    // 评论数
    private Integer commentCount;

    // 内容标签
    private List<TagVO> tags;

}

