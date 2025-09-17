package com.cc.talkpojo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
import java.io.Serializable;

import lombok.*;
import lombok.experimental.Accessors;

/**
 * <p>
 * 
 * </p>
 *
 * @author cc
 * @since 2025-09-10
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("like_record")
@Builder
public class LikeRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 被点赞的对象ID
     */
    private Long targetId;

    /**
     * 点赞对象类型
     */
    private String targetType;

    /**
     * 点赞用户ID
     */
    private Long userId;

    /**
     * 点赞时间
     */
    private LocalDateTime createTime;

}
