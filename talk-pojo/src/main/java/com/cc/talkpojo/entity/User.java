package com.cc.talkpojo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
import java.io.Serializable;

import lombok.*;
import lombok.experimental.Accessors;

/**
 * <p>
 * 用户登录账号信息表
 * </p>
 *
 * @author cc
 * @since 2025-07-01
 */
@Data
@Builder
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@TableName("user")
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户名（登录用，唯一）
     */
    private String username;

    /**
     * 加密后的密码
     */
    private String password;

    /**
     * 邮箱
     */
    private String email;


    /**
     * 手机号（用于展示或绑定）
     */
    private String phone;

    /**
     * 账号状态：1正常，0禁用
     */
    private Integer status;

    /**
     * 用户角色：user/admin
     */
    private String role;

    /**
     * 账号创建时间
     */
    private LocalDateTime createTime;

    /**
     * 账号信息最后修改时间
     */
    private LocalDateTime updateTime;
}
