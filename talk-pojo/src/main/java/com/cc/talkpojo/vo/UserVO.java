package com.cc.talkpojo.vo;


import lombok.Data;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class UserVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户id */
    private Long userId;

    /** 用户名（可用于展示） */
    private String username;

    /** 昵称 */
    private String nickname;

    /** 头像地址 */
    private String avatar;

    /** 邮箱（可选） */
    private String email;

    /** 手机号（可选） */
    private String phone;

    /** 角色（一般 user/admin） */
    private String role;

    /** 性别（M/F/O） */
    private String gender;

    /** 生日 */
    private LocalDate birthday;

    /** 地区 */
    private String region;

    /** 个性签名 */
    private String signature;

    /** 背景图 */
    private String background;

    /** 用户等级 */
    private Integer level;

    /** 经验值 */
    private Integer experience;

    /** 账号创建时间 */
    private LocalDateTime createTime;

    /** 最后更新时间 */
    private LocalDateTime updateTime;

}
