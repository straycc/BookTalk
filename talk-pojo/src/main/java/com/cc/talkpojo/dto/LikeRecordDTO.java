package com.cc.talkpojo.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor

public class LikeRecordDTO {


    private Long id;

    /**
     * 被点赞的对象ID
     */
    private Long targetId;

    /**
     * 点赞对象类型
     */
    private String likeTargetType; // 可选值  BOOK_REVIEW, COMMNET, BOOKLIST

    /**
     * 点赞用户ID
     */
    private Long userId;

}
