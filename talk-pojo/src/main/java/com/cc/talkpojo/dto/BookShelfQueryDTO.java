package com.cc.talkpojo.dto;

import lombok.Data;

/**
 * 书架查询DTO
 *
 * @author cc
 * @since 2025-10-12
 */
@Data
public class BookShelfQueryDTO {

    /**
     * 页码
     */
    private Integer page = 1;

    /**
     * 每页数量
     */
    private Integer size = 10;

    /**
     * 阅读状态筛选
     * WANT_TO_READ-想读, READING-在读, READ-读完
     */
    private String status;

    /**
     * 书籍名称搜索
     */
    private String bookName;

    /**
     * 排序方式
     * CREATE_TIME-添加时间, BOOK_NAME-书名
     */
    private String sortBy = "CREATE_TIME";

    /**
     * 排序方向
     * ASC-升序, DESC-降序
     */
    private String sortOrder = "DESC";
}