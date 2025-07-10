package com.cc.talkpojo.dto;


import lombok.Data;

import java.time.LocalDate;

@Data
public class PageUserDTO implements PageDTO{

    /** 查询页面*/
    private Integer pageNum = 1;

    /** 每页条数*/
    private Integer pageSize = 10;

    /** 用户名*/
    private String username;

    /** 账号状态（1:正常，0:封禁）*/
    private Integer status;

    /** 用户角色（user,admin）*/
    private String role;

    /** 起始时间*/
    private LocalDate createdFrom;

    /** 结束时间*/
    private LocalDate createdTo;
}
