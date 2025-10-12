package com.cc.talkpojo.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 添加书籍到书架DTO
 *
 * @author cc
 * @since 2025-10-12
 */
@Data
public class BookShelfAddDTO {

    /**
     * 书籍ID
     */
    @NotNull(message = "书籍ID不能为空")
    private Long bookId;

    /**
     * 阅读状态
     * WANT_TO_READ-想读, READING-在读, READ-读完
     */
    private String status = "WANT_TO_READ";
}