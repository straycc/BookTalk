package com.cc.booktalk.entity.entity.review;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.io.Serializable;

/**
 * <p>
 * 图书评论表
 * </p>
 *
 * @author cc
 * @since 2025-07-09
 */
@TableName("book_review")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BookReview implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 评论ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
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
     * 热度值
     */
    private double hotScore;

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

    /**
     * 热度更新时间
     */
    private LocalDateTime hotScoreUpdateTime;

}
