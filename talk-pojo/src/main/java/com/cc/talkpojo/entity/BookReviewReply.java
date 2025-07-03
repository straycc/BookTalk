package com.cc.talkpojo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 书评回复表
 * </p>
 *
 * @author cc
 * @since 2025-06-30
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("book_review_reply")
public class BookReviewReply implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 回复ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 主评论ID
     */
    private Long reviewId;

    /**
     * 回复人ID
     */
    private Long userId;

    /**
     * 回复内容
     */
    private String content;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;


}
