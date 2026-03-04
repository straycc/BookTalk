package com.cc.booktalk.entity.entity.recommendation;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.*;
import java.time.LocalDateTime;
import java.io.Serializable;

/**
 * 用户兴趣标签实体类
 * 用于存储用户对不同标签的兴趣分数
 * 是推荐算法计算的核心数据
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
     * 标签名称
     * 例如：Java、编程、小说、历史、科幻、技术等
     */
    private String tagName;

    /**
     * 兴趣分数
     * 基于用户行为计算得出的兴趣强度
     * 分数越高表示兴趣越强烈
     * 分数会根据用户行为和时间衰减动态更新
     */
    private Double interestScore;

    /**
     * 行为次数
     * 该兴趣标签相关的用户行为总次数
     * 用于兴趣分数计算和权重调整
     */
    private Integer behaviorCount;

    /**
     * 最后更新时间
     * 兴趣分数最后更新的时间
     * 用于时间衰减计算和数据新鲜度判断
     */
    private LocalDateTime updateTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}