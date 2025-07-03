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
 * 评论审核日志表
 * </p>
 *
 * @author cc
 * @since 2025-06-30
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("book_review_audit_log")
public class BookReviewAuditLog implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 评论ID
     */
    private Long reviewId;

    /**
     * 审核管理员ID
     */
    private Long adminId;

    /**
     * 审核结果：0-驳回，1-通过
     */
    private Integer result;

    /**
     * 审核备注
     */
    private String remark;

    /**
     * 审核时间
     */
    private LocalDateTime auditTime;


}
