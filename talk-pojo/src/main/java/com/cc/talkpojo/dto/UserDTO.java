package com.cc.talkpojo.dto;

import lombok.*;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Builder
@Data
@AllArgsConstructor(access = AccessLevel.PUBLIC)
@NoArgsConstructor
public class UserDTO implements Serializable {

    private static final long serialVersionUID = 1L;


    /**
     * 用户id
     */
    private Long id;

    /**
     * 用户名（登录用，唯一）
     */
    private String username;

    /**
     * 密码
     */
    private String password;


    /**
     * 邮箱
     */
    private String email;

    /**
     * 手机号
     */
    private String phone;


    /**
     * 账号状态：1正常，0禁用
     */
    private Integer status;


}

