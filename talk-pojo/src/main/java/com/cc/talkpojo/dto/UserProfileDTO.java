package com.cc.talkpojo.dto;


import lombok.Data;

import java.time.LocalDate;

@Data
public class UserProfileDTO {
    /**
     * 用户id
     */
    private Long userId;
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


    /**
     * 昵称（可重复）
     */
    private String nickname;
    /**
     * 头像地址
     */
    private String avatar;
    /**
     * 背景图地址
     */
    private String background;

    /**
     * 性别（M男/F女/O其他）
     */
    private String gender;
    /**
     * 生日
     */
    private LocalDate birthday;
    /**
     * 所在地区
     */
    private String region;
    /**
     * 个性签名
     */
    private String signature;
    /**
     * 用户等级
     */
    private Integer level;
    /**
     * 经验值
     */
    private Integer experience;



}
