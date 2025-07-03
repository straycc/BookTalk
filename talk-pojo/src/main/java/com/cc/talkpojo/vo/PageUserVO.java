package com.cc.talkpojo.vo;

import lombok.Data;

import java.time.LocalDate;

@Data
public class PageUserVO {
    /** 用户id*/
    private Long id;

    /** 用户名*/
    private String username;

    /** 用户昵称*/
    private String nickname;

    /** 手机号*/
    private String phone;

    /** 用户邮箱*/
    private String email;

    /** 账号状态（1:正常，0:封禁）*/
    private Integer status;

    /** 用户角色（user,admin）*/
    private String role;

    /** 注册时间的起始日期*/
    private LocalDate createdFrom;


}
