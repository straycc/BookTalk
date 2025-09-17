package com.cc.talkpojo.dto;


import lombok.Data;

@Data
public class CommentPageDTO implements PageDTO{

    /** 页面号 */
    private Integer pageNum = 1;

    /** 页面大小 */
    private Integer pageSize = 10;


    private Long userId;

    private String sortField = "createTime";

    private String sortOrder = "desc"; // asc | desc


}
