package com.cc.talkpojo.dto;


import lombok.Data;

@Data
public class LikePageDTO implements PageDTO{
    private Long userId;
    private Integer pageNum = 1;
    private Integer pageSize = 10;
    private String sortField = "createTime";
}
