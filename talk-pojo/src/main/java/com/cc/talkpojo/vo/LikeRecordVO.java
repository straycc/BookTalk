package com.cc.talkpojo.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class LikeRecordVO {

    private Long id;

    /**
     * 被点赞的对象ID
     */
    private Long targetId;

    /**
     * 被点赞对象类型
     */
    private String targetType;

    /**
     * 被点赞对象内容
     */
    private String targetContent; // 书名/书评标题/评论内容

    /**
     * 被点赞作者id
     */
    private Long targetUserId;

    /**
     * 被点赞用户昵称
     */
    private String targetNickName;

    /**
     * 被点赞用户头像
     */
    private String targetUserAvatar;


    /**
     * 点赞用户id
     */
    private Long likeUserId;

    /**
     * 点赞用户昵称
     */
    private String nickName;

    /**
     * 点赞用户头像
     */
    private String avatar;

    /**
     * 点赞时间
     */
    private LocalDateTime createTime;


}
