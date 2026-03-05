package com.cc.booktalk.interfaces.dto.user.like;


import com.cc.booktalk.interfaces.dto.user.base.PageDTO;
import lombok.Data;

@Data
public class LikePageDTO implements PageDTO {
    private Long userId;
    private Integer pageNum = 1;
    private Integer pageSize = 10;
    private String sortField = "createTime";
}
