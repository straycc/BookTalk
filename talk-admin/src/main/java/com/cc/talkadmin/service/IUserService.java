package com.cc.talkadmin.service;

import com.cc.talkpojo.Result.PageResult;
import com.cc.talkpojo.dto.PageUserDTO;
import com.cc.talkpojo.dto.UserLoginDTO;
import com.cc.talkpojo.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cc.talkpojo.vo.UserLoginVO;

/**
 * <p>
 * 用户登录账号信息表 服务类
 * </p>
 *
 * @author cc
 * @since 2025-07-05
 */
public interface IUserService extends IService<User> {

    PageResult getPageUser(PageUserDTO pageUserDTO);

    UserLoginVO login(UserLoginDTO userLoginDTO);
}
