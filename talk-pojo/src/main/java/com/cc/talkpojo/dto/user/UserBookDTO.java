package com.cc.talkpojo.dto.user;


import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class UserBookDTO implements Serializable {

    private static final long serialVersionUID = 1L;


    /**
     * 图书ID
     */
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
     * 图书简介
     */
    private String description;

    /**
     * 作者
     */
    private String author;


    /**
     * 封面图片URL
     */
    private String coverUrl;


    /**
     * 分类ID（单分类）
     */
    private Long categoryId;



}
