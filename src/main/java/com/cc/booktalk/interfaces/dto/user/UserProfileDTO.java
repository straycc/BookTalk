package com.cc.booktalk.interfaces.dto.user;


import lombok.Data;

import java.time.LocalDate;
import javax.validation.constraints.Email;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

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
     * 邮箱
     */
    @Email(message = "邮箱格式不正确")
    private String email;
    /**
     * 手机号
     */
    @Pattern(regexp = "^$|^1\\d{10}$", message = "手机号格式不正确")
    private String phone;


    /**
     * 昵称（可重复）
     */
    @Size(max = 30, message = "昵称不能超过30个字符")
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
    @Size(max = 200, message = "个性签名不能超过200个字符")
    private String signature;



}
