package com.cc.booktalk.entity.vo;

import lombok.*;
import java.time.LocalDateTime;

/**
 * 个性化推荐视图对象
 * 用于向前端返回个性化推荐结果，包含推荐图书和推荐相关信息
 *
 * @author cc
 * @since 2024-01-15
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PersonalizedRecVO {

    /**
     * 推荐记录ID
     */
    private Long recommendationId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 图书ID
     */
    private Long bookId;

    /**
     * 书籍标题
     */
    private String bookTitle;

    /**
     * 书籍作者
     */
    private String author;

    /**
     * 书籍封面
     */
    private String bookCover;


    /**
     * 推荐分数
     * 推荐算法计算得出的综合分数，用于排序
     * 分数越高，推荐优先级越高
     */
    private Double score;

    /**
     * 推荐理由
     * 向用户解释为什么推荐这本书
     * 例如："基于您喜欢的科幻类图书推荐"、"喜欢这本书的用户也喜欢"等
     */
    private String reason;

    /**
     * 推荐算法类型
     * 标识推荐算法的来源，用于算法效果分析
     * - CONTENT_BASED: 基于内容的推荐
     * - COLLABORATIVE: 协同过滤推荐
     * - HYBRID: 混合推荐
     * - POPULAR: 热门推荐（冷启动）
     */
    private String algorithmType;

    /**
     * 推荐时间
     */
    private LocalDateTime recommendTime;

    /**
     * 图书基本信息
     * 包含推荐的图书的详细信息
     */
    private BookRankingVO bookInfo;

    /**
     * 推荐置信度
     * 推荐结果的可信程度，0-1之间
     * 基于用户行为数据量和算法稳定性计算
     */
    private Double confidence;

    /**
     * 相关兴趣标签
     * 触发推荐的用户兴趣标签
     * 用于向用户展示推荐依据
     */
    private String relatedInterests;

    /**
     * 是否已读
     * 用户是否已经阅读过这本书
     */
    private Boolean isRead;

    /**
     * 是否已收藏
     * 用户是否已经收藏这本书
     */
    private Boolean isCollected;

    /**
     * 推荐有效期
     * 推荐结果的有效时间，超过时间后重新计算
     */
    private LocalDateTime expireTime;
}