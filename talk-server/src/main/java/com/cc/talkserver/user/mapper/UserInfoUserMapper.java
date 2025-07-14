package com.cc.talkserver.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cc.talkpojo.entity.UserInfo;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 用户扩展资料表（展示/编辑） Mapper 接口
 * </p>
 *
 * @author cc
 * @since 2025-07-01
 */
@Mapper
public interface UserInfoUserMapper extends BaseMapper<UserInfo> {

}
