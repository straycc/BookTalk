package com.cc.talkserver.admin.mapper;

import com.cc.talkpojo.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 用户登录账号信息表 Mapper 接口
 * </p>
 *
 * @author cc
 * @since 2025-07-14
 */
@Mapper
public interface UserAdminMapper extends BaseMapper<User> {

}
