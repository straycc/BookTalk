package com.cc.talkpojo.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

/**
 * 榜单查询参数
 *
 * @author cc
 * @since 2025-10-13
 */
@Data
public class RankingQueryDTO {

    /**
     * 榜单类型
     */
    private String rankingType;

    /**
     * 时间周期
     */
    private String rankingPeriod;

    /**
     * 书籍分类
     */
    private String category;

    /**
     * 页码
     */
    @Min(value = 1, message = "页码最小为1")
    private Integer page = 1;

    /**
     * 每页大小
     */
    @Min(value = 1, message = "每页大小最小为1")
    @Max(value = 100, message = "每页大小最大为100")
    private Integer size = 10;
}