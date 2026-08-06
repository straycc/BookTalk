package com.cc.booktalk.interfaces.dto.user.square;

import com.cc.booktalk.interfaces.dto.user.base.PageDTO;
import lombok.Data;

@Data
public class SquareFeedQueryDTO implements PageDTO {

    private Integer pageNum = 1;

    private Integer pageSize = 10;

    private String type = "all";

    private String sort = "latest";

    private Long tagId;
}
