package com.cc.booktalk.domain.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI 推荐结果中的图书摘要。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiRecommendedBook {

    private Long bookId;

    private String bookTitle;

    private String author;

    private String bookCover;

    private String reason;

    private Double confidence;

    private Double score;
}
