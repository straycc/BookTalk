package com.cc.booktalk.entity.dto.search;

import com.cc.booktalk.entity.dto.base.PageDTO;
import lombok.Data;

@Data
public class PageSearchDTO implements PageDTO {
    private String keyword;
    private Integer pageNum = 1;
    private Integer pageSize = 10;
}
