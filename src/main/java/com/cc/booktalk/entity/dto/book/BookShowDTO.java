package com.cc.booktalk.entity.dto.book;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookShowDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 图书ID */
    private Long id;

    /** 封面URL */
    private String coverUrl;

    /** 书名 */
    private String title;

    /** 作者（可包含国籍） */
    private String author;

    /** 评分（可选） */
    private Double averageScore;

    /** 标签/分类（可选） */
    private String categoryId;
}