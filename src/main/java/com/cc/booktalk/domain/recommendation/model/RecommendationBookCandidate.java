package com.cc.booktalk.domain.recommendation.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 推荐候选书籍
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationBookCandidate {

    private Long bookId;
    private String bookTitle;
    private String author;
    private String bookCover;
    private Long categoryId;
    private String categoryName;
    private Double averageScore;
    private Integer scoreCount;
    private Integer favoriteCount;
    private Double hotScore;
    private LocalDateTime createTime;

    @Builder.Default
    private Set<String> matchedTags = new LinkedHashSet<>();

    @Builder.Default
    private Set<String> matchedCategories = new LinkedHashSet<>();

    @Builder.Default
    private Set<String> matchedAuthors = new LinkedHashSet<>();

    @Builder.Default
    private Set<String> sourceStrategies = new LinkedHashSet<>();

    @Builder.Default
    private Set<String> reasonCodes = new LinkedHashSet<>();

    private Double score;
    private Double confidence;
    private String reason;
    private LocalDateTime recommendTime;
}
