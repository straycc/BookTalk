package com.cc.booktalk.application.admin.service;

import com.cc.booktalk.common.result.PageResult;
import com.cc.booktalk.interfaces.dto.admin.PageUserDTO;
import com.cc.booktalk.interfaces.dto.user.UserLoginDTO;
import com.cc.booktalk.domain.entity.user.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cc.booktalk.interfaces.vo.user.user.UserLoginVO;

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
