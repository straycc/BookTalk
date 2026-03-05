package com.cc.booktalk.interfaces.dto.user.search;

import com.cc.booktalk.interfaces.dto.user.base.PageDTO;
import lombok.Data;

@Data
public class PageSearchDTO implements PageDTO {
    private String keyword;
    private Integer pageNum = 1;
    private Integer pageSize = 10;
}
