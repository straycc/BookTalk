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
 * 图书评论表
 * </p>
 *
 * @author cc
 * @since 2025-06-30
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("book_review")
public class BookReview implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 评论ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

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

    /**
     * 状态：1-待审核，2-已通过，0-驳回/屏蔽
     */
    private Integer status;

    /**
     * 审核备注
     */
    private String auditRemark;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 修改时间
     */
    private LocalDateTime updateTime;


}
