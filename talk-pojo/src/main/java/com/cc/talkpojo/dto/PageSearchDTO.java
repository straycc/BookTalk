package com.cc.talkpojo.dto;

import lombok.Data;

@Data
public class PageSearchDTO implements PageDTO{
    private String keyword;
    private Integer pageNum = 1;
    private Integer pageSize = 10;
}
