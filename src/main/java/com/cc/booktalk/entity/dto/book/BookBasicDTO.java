package com.cc.booktalk.entity.dto.book;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BookBasicDTO {
    private Long id;
    private String isbn;
    private String title;
    private String author;
    private String coverUrl;
    private Long categoryId;
    private Double averageScore;
    private Integer scoreCount;
}