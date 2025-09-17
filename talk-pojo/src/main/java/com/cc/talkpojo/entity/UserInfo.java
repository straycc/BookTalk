package com.cc.talkpojo.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import java.time.LocalDate;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
import java.io.Serializable;

import lombok.*;
import lombok.experimental.Accessors;

/**
 * <p>
 * 用户扩展资料表（展示/编辑）
 * </p>
 *
 * @author cc
 * @since 2025-07-01
 */
@Data
@Builder
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("user_info")
@AllArgsConstructor
@NoArgsConstructor
public class UserInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 关联的用户ID
     */
    private Long userId;

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

    /**
     * 资料创建时间
     */
    private LocalDateTime createTime;

    /**
     * 资料最后修改时间
     */
    private LocalDateTime updateTime;


}
