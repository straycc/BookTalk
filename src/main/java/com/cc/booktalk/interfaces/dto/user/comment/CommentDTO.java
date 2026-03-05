package com.cc.booktalk.interfaces.dto.user.comment;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CommentDTO {

    private Long id;

    /**
     * 评论目标ID
     */
    private Long rootId;

    /**
     * 评论目标类型: REVIEW, COMMENT
     */
    private String targetType;

    /**
     * 父评论ID，空表示直接评论书评
     */
    private Long parentId;

    /**
     * 评论人ID
     */
    private Long userId;

    /**
     * 评论内容
     */
    private String content;

}
