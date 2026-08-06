package com.cc.booktalk.domain.entity.post;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 广场讨论帖实体。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("post")
public class Post implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;

    private String title;

    private String content;

    private String summary;

    private Long relatedBookId;

    private Integer viewCount;

    private Integer likeCount;

    private Integer commentCount;

    private Double hotScore;

    /**
     * 发布状态：1-公开，0-隐藏。
     */
    private Integer status;

    private LocalDateTime lastActiveTime;

    private LocalDateTime hotScoreUpdateTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
