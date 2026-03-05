package com.cc.booktalk.interfaces.controller.user;


import com.cc.booktalk.common.constant.Upload;
import com.cc.booktalk.common.constant.BusinessConstant;
import com.cc.booktalk.common.context.UserContext;
import com.cc.booktalk.common.exception.BaseException;
import com.cc.booktalk.common.result.Result;
import com.cc.booktalk.interfaces.dto.user.UserLoginDTO;
import com.cc.booktalk.interfaces.dto.user.UserProfileDTO;
import com.cc.booktalk.interfaces.dto.user.UserRegisterDTO;
import com.cc.booktalk.interfaces.vo.user.user.UserLoginVO;
import com.cc.booktalk.interfaces.vo.user.user.UserVO;
import com.cc.booktalk.application.user.service.user.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.validation.Valid;

/**
 * <p>
 * 用户基本信息表 前端控制器
 * </p>
 *
 * @author cc
 * @since 2025-06-30
 */
@RestController
@RequestMapping("/user")
@Api(tags = "用户相关接口")
@Slf4j

public class UserController {

    @Resource
    private UserService userService;


    /**
     * 用户注册
     * @param userRegisterDTO
     * @return
     */
    @ApiOperation("用户注册")
    @PostMapping("/register")
    public Result<Object> register(@Valid @RequestBody UserRegisterDTO userRegisterDTO){
        userService.register(userRegisterDTO);
        log.info("用户名:" + userRegisterDTO.getUsername() +", 注册成功!");
        return Result.success();
    }


    /**
     * 用户登录
     * @param UserLoginDTO
     * @return
     */
    @ApiOperation("用户登录")
    @PostMapping("/login")
    public Result<Object> login(@Valid @RequestBody UserLoginDTO UserLoginDTO) {
        log.info("用户："+UserLoginDTO.getUsername()+"请求登录!");
        UserLoginVO userLoginVO = userService.login(UserLoginDTO);
        return Result.success(userLoginVO);
    }


    /**
     * 根据id查询用户信息
     * @param userId
     * @return
     */
    @ApiOperation("用户id查询用户信息")
    @GetMapping("/profile/{userId}")
    public Result<UserVO> getById(@PathVariable Long userId){
        log.info("查询个人主页");
        UserVO userVO = userService.getProfile(userId);
        return Result.success(userVO);
    }

    /**
     * 修改用户信息
     * @param userProfileDTO
     * @return
     */
    @ApiOperation("修改用户基本信息")
    @PutMapping("/profile/user-info")
    public Result<Object> revise(@RequestBody UserProfileDTO userProfileDTO){
        log.info("用戶："+userProfileDTO.getUsername()+"修改个人信息。");
        userService.reviseProfile(userProfileDTO);
        return Result.success();
    }

    /**
     * 图片上传(avatar 或 background)
     * @param file
     * @param type
     * @return
     */
    @ApiOperation("文件上传")
    @PostMapping("/profile/upload-image")
    public Result<String> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam("type") String type
    ){
        if (!Upload.IMAGE_AVATAR.equals(type) && !Upload.IMAGE_BACKGROUND.equals(type)) {
            throw new BaseException(BusinessConstant.PARAM_ERROR);
        }
        String url = userService.uploadImage(file, type);
        return Result.success(url);
    }

    /**
     * 查询个人主页
     * @return
     */
    @ApiOperation("查询个人主页")
    @GetMapping("/me")
    public Result<UserVO> getMe(){
        Long userId = UserContext.getUser().getId();
        UserVO userVO = userService.getProfile(userId);
        return Result.success(userVO);
    }


    /**
     * 用户退出登录
     * @return
     */
    @ApiOperation("用户退出登录")
    @PostMapping("/unLogin")
    public Result<Object> unLogin(){
        UserContext.removeUser();
        return Result.success();
    }

    //TODO 待实现 ：用户等级/经验相关

}
