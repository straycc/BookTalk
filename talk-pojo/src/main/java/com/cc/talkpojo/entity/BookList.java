package com.cc.talkpojo.entity;

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
 * 用户书单表
 * </p>
 *
 * @author cc
 * @since 2025-09-17
 */
@TableName("book_list")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BookList implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 书单ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 创建者ID
     */
    private Long userId;

    /**
     * 书单标题
     */
    private String title;

    /**
     * 书单简介
     */
    private String description;

    /**
     * 封面图（可选）
     */
    private String coverUrl;

    /**
     * 是否公开（1公开/0私密）
     */
    private Integer visibility;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 修改时间
     */
    private LocalDateTime updateTime;


}
