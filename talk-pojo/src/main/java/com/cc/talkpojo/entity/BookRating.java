package com.cc.talkpojo.entity;

import com.baomidou.mybatisplus.annotation.TableField;
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
 * 图书评分表
 * </p>
 *
 * @author cc
 * @since 2025-07-04
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("book_rating")
public class BookRating implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 评分ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 图书ID
     */
    private Long bookId;

    /**
     * 评分，1-10分
     */
    private Integer score;

    /**
     * 是否收藏
     */
    private Boolean bookFavorite;
    /**
     * 评论内容（可选）
     */
    private String comment;

    /**
     * 评分时间
     */
    private LocalDateTime createTime;


}
