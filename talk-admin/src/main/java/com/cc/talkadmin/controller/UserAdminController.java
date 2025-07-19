package com.cc.talkadmin.controller;


import com.cc.talkcommon.result.Result;
import com.cc.talkpojo.Result.PageResult;
import com.cc.talkpojo.dto.admin.PageUserDTO;
import com.cc.talkpojo.dto.UserLoginDTO;
import com.cc.talkpojo.vo.UserLoginVO;
import com.cc.talkserver.admin.service.UserAdminService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;


import javax.annotation.Resource;
import javax.validation.Valid;

/**
 * <p>
 * 用户登录账号信息表 前端控制器
 * </p>
 *
 * @author cc
 * @since 2025-07-05
 */
@RestController
@RequestMapping("/admin")
@Api(tags = "用户管理接口")
@Slf4j
public class UserAdminController {


    @Resource
    private UserAdminService userAdminService;//talk-server


    @ApiOperation("用户登录")
    @PostMapping("/login")
    public Result<Object> login(@Valid @RequestBody UserLoginDTO UserLoginDTO) {
        log.info("用户："+UserLoginDTO.getUsername()+"请求登录!");
        UserLoginVO userLoginVO = userAdminService.login(UserLoginDTO);
        return Result.success(userLoginVO);
    }


    /**
     * 用户分页查询
     * @param pageUserDTO
     * @return
     */
    @ApiOperation("用户分页查询")
    @GetMapping("/page/user-list")
    public Result<PageResult> getPageUser(PageUserDTO pageUserDTO) {
        log.info("用户分也查询:");
        PageResult pageResult = userAdminService.getPageUser(pageUserDTO);
        return Result.success(pageResult);
    }



}
