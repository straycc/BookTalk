package com.cc.booktalk.entity.test;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BookTagRelationDTO {
    private Long bookId;
    private Long tagId;
    private String tagName;
}
