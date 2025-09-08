package com.cc.talkpojo.dto;

import lombok.Data;

@Data
public class PageReviewDTO  implements PageDTO {

    /** 页面号*/
    private Integer pageNum;

    /** 页面大小*/
    private Integer pageSize;

    /** 评论类型*/
    private Integer type;
}
