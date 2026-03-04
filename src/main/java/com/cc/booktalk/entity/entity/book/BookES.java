package com.cc.booktalk.entity.entity.book;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookES {

    private Long id;

    /**
     * ISBN编号
     */
    private String isbn;

    /**
     * 书名
     */
    private String title;

    /**
     * 副标题
     */
    private String subTitle;


    /**
     * 图书简介
     */
    private String description;

    /**
     * 作者
     */
    private String author;

    /**
     * 作者国籍
     */
    private String authorCountry;

    /**
     * 封面图片URL
     */
    private String coverUrl;

    /**
     * 分类ID（单分类）
     */
    private Long categoryId;

    /**
     * 平均评分
     */
    private Double averageScore;

    /**
     * 评分人数
     */
    private Integer scoreCount;

}
