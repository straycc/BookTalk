package com.cc.talkpojo.vo;

import lombok.Builder;
import lombok.Data;


@Data
@Builder
public class UserLoginVO {

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 登录token
     */
    private String token;

}
