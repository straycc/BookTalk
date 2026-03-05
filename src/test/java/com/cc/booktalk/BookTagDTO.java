package com.cc.booktalk;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class BookTagDTO {

    private String isbn;
    // 标签列表
    private List<String> tag;
}
