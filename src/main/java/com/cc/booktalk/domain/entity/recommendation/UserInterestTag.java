package com.cc.booktalk.domain.entity.recommendation;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户兴趣画像实体类
 * 用于存储用户对标签、分类、作者等不同维度的兴趣分数
 *
 * @author cc
 * @since 2024-01-15
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("user_interest_tag")
public class UserInterestTag implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 兴趣类型
     * 取值：TAG / CATEGORY / AUTHOR
     */
    private String interestType;

    /**
     * 兴趣键
     * TAG 类型保存标签名称，CATEGORY 类型保存分类ID，AUTHOR 类型保存作者名
     */
    private String interestKey;

    /**
     * 兴趣分数
     */
    private Double interestScore;

    /**
     * 行为次数
     */
    private Integer behaviorCount;

    /**
     * 最后更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
