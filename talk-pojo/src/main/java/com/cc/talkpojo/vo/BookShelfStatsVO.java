package com.cc.talkpojo.vo;

import lombok.Data;

/**
 * 书架统计VO
 *
 * @author cc
 * @since 2025-10-12
 */
@Data
public class BookShelfStatsVO {

    /**
     * 想读数量
     */
    private Integer wantToReadCount = 0;

    /**
     * 在读数量
     */
    private Integer readingCount = 0;

    /**
     * 读完数量
     */
    private Integer readCount = 0;

    /**
     * 总计数量
     */
    private Integer totalCount = 0;
}