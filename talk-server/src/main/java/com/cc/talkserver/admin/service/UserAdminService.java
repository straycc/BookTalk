package com.cc.talkserver.admin.service;

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
 * @since 2025-07-14
 */
public interface UserAdminService extends IService<User> {


    /**
     * 用户分页查询
     * @param pageUserDTO
     * @return
     */
    PageResult getPageUser(PageUserDTO pageUserDTO);


    /**
     * 用户登录
     * @param userLoginDTO
     * @return
     */
    UserLoginVO login(UserLoginDTO userLoginDTO);


}
