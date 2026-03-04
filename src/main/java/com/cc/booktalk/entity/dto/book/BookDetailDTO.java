package com.cc.booktalk.entity.dto.book;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
public class BookDetailDTO extends BookBasicDTO {
    private String subTitle;
    private String description;
    private String publisher;
    private LocalDate publishDate;
    private BigDecimal price;
    private Integer pageCount;
}
