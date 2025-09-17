package com.cc.talkpojo.vo;

import com.cc.talkpojo.enums.TargetType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommentVO {

    private Long id;

    /**
     * 评论目标ID
     */
    private Long targetId;

    /**
     * 评论目标类型: BOOKREVIEW, COMMENT, BOOKLIST
     */
    private String  targetType;

    /**
     * 父评论ID，空表示直接评论书评
     */
    private Long parentId;

    /**
     * 子评论列表
     */
    private List<CommentVO> replies = new ArrayList<>();

    /**
     * 评论人ID
     */
    private Long userId;


    /**
     * 用户昵称
     */
    private String nickName;


    /**
     * 用户头像
     */
    private String avatar;

    /**
     * 评论内容
     */
    private String content;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;


}
