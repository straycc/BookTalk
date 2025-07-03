package com.cc.talkuser.service.impl;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.digest.MD5;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.cc.talkcommon.constant.Upload;
import com.cc.talkcommon.constant.UserConstant;
import com.cc.talkcommon.context.UserContext;
import com.cc.talkcommon.exception.BaseException;
import com.cc.talkcommon.jwt.JwtUtil;
import com.cc.talkcommon.oss.AliOssUtil;
import com.cc.talkpojo.dto.UserDTO;
import com.cc.talkpojo.dto.UserLoginDTO;
import com.cc.talkpojo.dto.UserProfileDTO;
import com.cc.talkpojo.dto.UserRegisterDTO;
import com.cc.talkpojo.entity.User;
import com.cc.talkpojo.entity.UserInfo;
import com.cc.talkpojo.vo.UserLoginVO;
import com.cc.talkpojo.vo.UserVO;
import com.cc.talkuser.mapper.UserInfoMapper;
import com.cc.talkuser.mapper.UserMapper;
import com.cc.talkuser.service.IUserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;


/**
 * <p>
 * 用户基本信息表 服务实现类
 * </p>
 *
 * @author cc
 * @since 2025-06-30
 */

@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {



    @Resource
    private UserMapper userMapper;

    @Resource
    private AliOssUtil aliOssUtil;

    @Resource
    private UserInfoMapper userInfoMapper;


    /**
     * 用户注册
     * @param userRegisterDTO
     * @return
     */
    @Override
    public void register(UserRegisterDTO userRegisterDTO) {

        String username = userRegisterDTO.getUsername();
        String password = userRegisterDTO.getPassword();

        //2. 检查用户名是否存在
        LambdaQueryWrapper<User> query = Wrappers.lambdaQuery();
        query.eq(User::getUsername,userRegisterDTO.getUsername());
        User exists = userMapper.selectOne(query);
        if(exists != null){
            throw new BaseException("用户名已存在");
        }
        //3. 构建用户对象
        User user = User.builder()
                .username(username)
                .password(SecureUtil.md5(password))
                .status(1)
                .role("user")
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();

        userMapper.insert(user);

        //4. 用户默认资料
        UserInfo userInfo = UserInfo.builder()
                .userId(user.getId())
                .nickname("user_"+ RandomUtil.randomNumbers(5))
                .avatar(UserConstant.USER_AVATAR)
                .background(UserConstant.USER_BACKGROUND)
                .signature(UserConstant.USER_SIGNATURE)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();

        userInfoMapper.insert(userInfo);
    }

    /**
     * 用户登录
     * @param userLoginDTO
     */
    @Override
    public UserLoginVO login( UserLoginDTO userLoginDTO) {

        //判空操作通过@valid注解实现
        String username = userLoginDTO.getUsername();
        String password = userLoginDTO.getPassword();

        //1.检查用户是否存在
        LambdaQueryWrapper<User> query = Wrappers.lambdaQuery();
        query.eq(User::getUsername,username);
        User user = userMapper.selectOne(query);
        if(user == null){
            throw new BaseException("用户不存在");
        }

        //2.检查密码是否正确
        if(!user.getPassword().equals(SecureUtil.md5(password))){
            throw new BaseException("密码错误!");
        }

        //3.检查用户账号状态
        if(user.getStatus() == 0){
            throw new BaseException("账号冻结，无法登录!");
        }

        //4.用户信息存储threadLocal
        UserDTO userDTO = new UserDTO();
        BeanUtils.copyProperties(user,userDTO);
        UserContext.saveUser(userDTO);

        //5.构造JWT令牌
        String token = JwtUtil.generateToken(userDTO);

        //6.返回登录成功数据
        return UserLoginVO.builder()
                .userId(userDTO.getId())
                .username(userDTO.getUsername())
                .token(token)
                .build();
    }

    /**
     * 查询个人主页
     * @return
     */
    @Override
    public UserVO getProfile(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BaseException("用户不存在");
        }

        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);

        return userVO;
    }


    /**
     * 修改用户基本资料
     * @param userProfileDTO
     */
    @Transactional
    @Override
    public void reviseProfile(UserProfileDTO userProfileDTO) {
        Long currentId  = UserContext.getUser().getId();
        if(userProfileDTO.getUserId() == null || !userProfileDTO.getUserId().equals(currentId)){
            throw new BaseException("非法操作，不允许修改他人信息！");
        }
        User user = User.builder()
                .id(userProfileDTO.getUserId())
                .username(userProfileDTO.getUsername())
                .password(SecureUtil.md5(userProfileDTO.getPassword()))
                .email(userProfileDTO.getEmail())
                .phone(userProfileDTO.getPhone())
                .updateTime(LocalDateTime.now())
                .build();

        // 更新 user_info 表
        UserInfo userInfo = UserInfo.builder()
                .userId(userProfileDTO.getUserId())
                .nickname(userProfileDTO.getNickname())
                .gender(userProfileDTO.getGender())
                .birthday(userProfileDTO.getBirthday())
                .region(userProfileDTO.getRegion())
                .signature(userProfileDTO.getSignature())
                .avatar(userProfileDTO.getAvatar())
                .background(userProfileDTO.getBackground())
                .build();
        userMapper.updateById(user);
        userInfoMapper.updateById(userInfo);
    }


    /**
     * 图片上传
     * @param file
     * @param imageType
     * @return
     */
    @Override
    public String uploadImage(MultipartFile file, String imageType) {
        if (file == null) {
            throw new BaseException("文件为空!");
        }
        log.info("文件上传:{}", file.getOriginalFilename());

        //获取原始文件名
        String originalFileName = file.getOriginalFilename();
        try {
            //文件名清理
            if (originalFileName != null) {
                // 第一步：替换非法字符为 _
                originalFileName = originalFileName.replaceAll("[^a-zA-Z0-9.]", "_");

                // 第二步：合并连续的下划线为一个
                originalFileName = originalFileName.replaceAll("_+", "_");
            }
            //获取文件名后缀
            String suffix = "";
            if (originalFileName != null && originalFileName.contains(".")) {
                suffix = originalFileName.substring(originalFileName.lastIndexOf("."));
            }
            //构造objectName
            String appType = Upload.APP_TYPE_USER;
            String userId = UserContext.getUser().getId().toString();
            String uuid = UUID.randomUUID().toString().replaceAll("-", "");
            String objectName = String.format("%s/%s/%s/%s%s", appType, userId, imageType, uuid, suffix);

            return aliOssUtil.upload(file.getBytes(), objectName);
        } catch (Exception e) {
            log.error("文件上传异常：{}", e.getMessage(), e);
            throw new BaseException("文件上传异常，请联系管理员");
        }
    }




}
