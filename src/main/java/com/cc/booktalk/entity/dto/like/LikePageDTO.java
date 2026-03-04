package com.cc.booktalk.entity.dto.like;


import com.cc.booktalk.entity.dto.base.PageDTO;
import lombok.Data;

@Data
public class LikePageDTO implements PageDTO {
    private Long userId;
    private Integer pageNum = 1;
    private Integer pageSize = 10;
    private String sortField = "createTime";
}
