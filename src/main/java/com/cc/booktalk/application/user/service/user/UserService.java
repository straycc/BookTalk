package com.cc.booktalk.application.user.service.user;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cc.booktalk.entity.dto.user.UserLoginDTO;
import com.cc.booktalk.entity.dto.user.UserProfileDTO;
import com.cc.booktalk.entity.dto.user.UserRegisterDTO;
import com.cc.booktalk.entity.entity.user.User;
import com.cc.booktalk.entity.vo.user.UserLoginVO;
import com.cc.booktalk.entity.vo.user.UserVO;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;

/**
 * <p>
 * 用户基本信息表 服务类
 * </p>
 *
 * @author cc
 * @since 2025-06-30
 */
public interface UserService extends IService<User> {


    /**
     * 用户注册
     * @param userRegisterDTO
     * @return
     */
    void register(UserRegisterDTO userRegisterDTO);


    /**
     * 用户登录
     * @param userLoginDTO
     */
    UserLoginVO login(@Valid UserLoginDTO userLoginDTO);


    /**
     * 查询个人主页
     * @return
     */
    UserVO getProfile(Long userId);


    /**
     * 修改个人信息
     * @param userProfileDTO
     */
    void reviseProfile(UserProfileDTO userProfileDTO);


    /**
     * 用户上传图片
     * @param file
     * @return
     */
    String uploadImage(MultipartFile file, String imageType);



}
