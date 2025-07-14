package com.cc.talkserver.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cc.talkpojo.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 用户基本信息表 Mapper 接口
 * </p>
 *
 * @author cc
 * @since 2025-06-30
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

}
