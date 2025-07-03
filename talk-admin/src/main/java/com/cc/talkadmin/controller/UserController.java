package com.cc.talkadmin.controller;


import com.cc.talkadmin.service.IUserService;
import com.cc.talkcommon.result.Result;
import com.cc.talkpojo.Result.PageResult;
import com.cc.talkpojo.dto.PageUserDTO;
import com.cc.talkpojo.vo.PageUserVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.stereotype.Controller;

/**
 * <p>
 * 用户登录账号信息表 前端控制器
 * </p>
 *
 * @author cc
 * @since 2025-07-05
 */
@Controller
@RequestMapping("/admin")

@Api(tags = "用户管理相关接口")
public class UserController {


    private static final Logger log = LoggerFactory.getLogger(UserController.class);
    private IUserService userService;

    /**
     * 用户分页查询
     * @param pageUserDTO
     * @return
     */
    @ApiOperation("用户分页查询")
    @GetMapping("/page/user-list")
    public Result<PageResult> getPageUser(PageUserDTO pageUserDTO) {
        log.info("用户分也查询:");
        PageResult pageResult = userService.getPageUser(pageUserDTO);
        return Result.success(pageResult);
    }







}
