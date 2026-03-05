package com.cc.booktalk.application.user.service.user.impl;

import cn.hutool.crypto.SecureUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cc.booktalk.common.constant.BusinessConstant;
import com.cc.booktalk.common.constant.RedisCacheConstant;
import com.cc.booktalk.common.constant.Upload;
import com.cc.booktalk.common.constant.UserConstant;
import com.cc.booktalk.common.context.UserContext;
import com.cc.booktalk.common.converter.UserConverter;
import com.cc.booktalk.common.exception.BaseException;
import com.cc.booktalk.common.jwt.JwtUtil;
import com.cc.booktalk.common.oss.AliOssUtil;
import com.cc.booktalk.common.utils.UserUtils;
import com.cc.booktalk.interfaces.dto.user.UserDTO;
import com.cc.booktalk.interfaces.dto.user.UserLoginDTO;
import com.cc.booktalk.interfaces.dto.user.UserProfileDTO;
import com.cc.booktalk.interfaces.dto.user.UserRegisterDTO;
import com.cc.booktalk.domain.entity.user.User;
import com.cc.booktalk.domain.entity.user.UserInfo;
import com.cc.booktalk.interfaces.vo.user.user.UserLoginVO;
import com.cc.booktalk.interfaces.vo.user.user.UserVO;
import com.cc.booktalk.infrastructure.persistence.user.mapper.recommendation.UserInfoUserMapper;
import com.cc.booktalk.infrastructure.persistence.user.mapper.user.UserMapper;
import com.cc.booktalk.application.user.service.user.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;


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
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {



    @Resource
    private UserMapper userMapper;

    @Resource
    private AliOssUtil aliOssUtil;

    @Resource
    private UserInfoUserMapper userInfoUserMapper;


    @Resource
    private RedisTemplate<String, String> customStringRedisTemplate;

    @Resource
    private RedisTemplate<String, Object> customObjectRedisTemplate;


    /**
     * 用户注册
     * @param userRegisterDTO
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(UserRegisterDTO userRegisterDTO) {

        String username = userRegisterDTO.getUsername();
        String password = userRegisterDTO.getPassword();

        //2. 检查用户名是否存在
        LambdaQueryWrapper<User> query = Wrappers.lambdaQuery();
        query.eq(User::getUsername,userRegisterDTO.getUsername());
        User exists = userMapper.selectOne(query);
        if(exists != null){
            throw new BaseException(BusinessConstant.USERNAME_REPEAT);
        }
        //3. 构建用户对象
        User user = User.builder()
                .username(username)
                .password(SecureUtil.md5(password))
                .email(userRegisterDTO.getEmail())
                .status(1)//当前账户状态
                .role(BusinessConstant.USER_ROLE_USER)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();

        userMapper.insert(user);

        //4. 构建用户详细信息
        UserInfo userInfo = UserInfo.builder()
                .userId(user.getId())
                .nickname(UserUtils.defaultNickname(user.getId()))
                .avatarUrl(UserConstant.USER_DEFAULT_AVATAR)
                .background(UserConstant.USER_DEFAULT_BACKGROUND)
                .signature((UserConstant.USER_DEFAULT_SIGNATURE))
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();

        userInfoUserMapper.insert(userInfo);
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
            throw new BaseException(UserConstant.USER_NOT_EXIST);
        }

        //2.检查密码是否正确
        if(!user.getPassword().equals(SecureUtil.md5(password))){
            throw new BaseException(UserConstant.PASSWORD_ERROR);
        }

        //3.检查用户账号状态
        if(user.getStatus() == 0){
            throw new BaseException(UserConstant.ACCOUNT_ERROR);
        }

        //4.用户信息存储threadLocal
        // 查询用户info
        UserInfo userInfo = userInfoUserMapper.selectOne(
                new LambdaQueryWrapper<UserInfo>().eq(UserInfo::getUserId, user.getId())
        );
        UserDTO userDTO = UserConverter.toUserDTO(user,userInfo);
        UserContext.saveUser(userDTO);
        //5.构造JWT令牌
        String token = JwtUtil.generateToken(userDTO);

        //6. 缓存用户信息到redis
        String key = RedisCacheConstant.USER_INFO_KEY_PREFIX + user.getId();
        UserVO userVO = UserConverter.toUserVO(user, userInfo);
        customObjectRedisTemplate.opsForValue().set(
                key,
                userVO,
                60,
                TimeUnit.MINUTES
        );

        //7. 返回登录成功信息
       UserLoginVO userLoginVO = UserConverter.toUserLoginVO(user,userInfo,token);

        //6.返回登录成功数据
        return userLoginVO;
    }

    /**
     * 查询个人主页
     * @return
     */
    @Override
    public UserVO getProfile(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BaseException(UserConstant.USER_NOT_EXIST);
        }
        UserInfo userInfo = userInfoUserMapper.selectById(user.getId());
        UserVO userVO = UserConverter.toUserVO(user, userInfo);
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
            throw new BaseException(BusinessConstant.WITH_NO_AUTHORITION);
        }
        User user = User.builder()
                .id(userProfileDTO.getUserId())
                .username(userProfileDTO.getUsername())
                .email(userProfileDTO.getEmail())
                .phone(userProfileDTO.getPhone())
                .updateTime(LocalDateTime.now())
                .build();

        // 更新 user_info 表
        UserInfo userInfo = UserInfo.builder()
                .userId(userProfileDTO.getUserId())
                .nickname(userProfileDTO.getNickname())
                .gender(userProfileDTO.getGender())
                .signature(userProfileDTO.getSignature())
                .avatarUrl(userProfileDTO.getAvatar())
                .background(userProfileDTO.getBackground())
                .build();
        userMapper.updateById(user);
        userInfoUserMapper.updateById(userInfo);
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
