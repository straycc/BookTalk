package com.cc.talkpojo.vo.user;


import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class UserBookVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 图书ID
     */
    private Long id;

    /**
     * 书名
     */
    private String title;

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
     * 平均评分
     */
    private BigDecimal averageScore;

}
