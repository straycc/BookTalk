package com.cc.talkpojo.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 书架项VO
 *
 * @author cc
 * @since 2025-10-12
 */
@Data
public class BookShelfVO implements Serializable {

    /**
     * 书架ID
     */
    private Long id;

    /**
     * 书籍ID
     */
    private Long bookId;

    /**
     * 书籍名称
     */
    private String bookName;

    /**
     * 书籍封面
     */
    private String bookCover;

    /**
     * 作者
     */
    private String author;

    /**
     * 出版社
     */
    private String publisher;

    /**
     * 阅读状态
     */
    private String status;

    /**
     * 状态描述
     */
    private String statusDesc;

    /**
     * 添加时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}