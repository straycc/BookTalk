package com.cc.booktalk.interfaces.vo.user.like;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor

public class LikeVO {
    /**
     * 点赞类型
     */
    private String targetType;

    /**
     * 点赞目标id
     */
    private Long targetId;

    /**
     * 点赞用户id
     */
    private Long userId;

    /**
     * 点赞数量
     */
    private int likeCount;

}


