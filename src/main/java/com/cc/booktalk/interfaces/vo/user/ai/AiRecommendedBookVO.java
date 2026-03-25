package com.cc.booktalk.interfaces.vo.user.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI 推荐书籍视图对象。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiRecommendedBookVO {

    private Long bookId;

    private String bookTitle;

    private String author;

    private String bookCover;

    private String reason;

    private Double confidence;

    private Double score;
}
