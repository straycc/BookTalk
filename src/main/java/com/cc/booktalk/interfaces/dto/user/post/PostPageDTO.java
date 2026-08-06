package com.cc.booktalk.interfaces.dto.user.post;

import com.cc.booktalk.interfaces.dto.user.base.PageDTO;
import lombok.Data;

@Data
public class PostPageDTO implements PageDTO {

    private Integer pageNum = 1;

    private Integer pageSize = 10;

    private String sort = "latest";

    private Long tagId;
}
