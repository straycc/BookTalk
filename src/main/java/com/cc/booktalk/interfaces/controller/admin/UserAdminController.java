package com.cc.booktalk.interfaces.controller.admin;


import com.cc.booktalk.common.result.Result;
import com.cc.booktalk.common.result.PageResult;
import com.cc.booktalk.interfaces.dto.admin.PageUserDTO;
import com.cc.booktalk.interfaces.dto.user.UserLoginDTO;
import com.cc.booktalk.interfaces.vo.user.user.UserLoginVO;
import com.cc.booktalk.application.admin.service.UserAdminService;
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
    private UserAdminService userAdminService;


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
