package com.cc.booktalk.entity.dto.book;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor

public class BookStatsDTO {
    private Long bookId;
    private Double averageScore;
    private Integer scoreCount;
    private Integer favoriteCount;
    private BigDecimal stars5Top;
    private BigDecimal stars4Top;
    private BigDecimal stars3Top;
    private BigDecimal stars2Top;
    private BigDecimal stars1Top;
}
