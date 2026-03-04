package com.cc.booktalk.application.admin.service.impl;
import cn.hutool.crypto.SecureUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.cc.booktalk.common.context.UserContext;
import com.cc.booktalk.common.exception.BaseException;
import com.cc.booktalk.common.jwt.JwtUtil;
import com.cc.booktalk.common.utils.ConvertUtils;
import com.cc.booktalk.entity.result.PageResult;
import com.cc.booktalk.entity.dto.admin.PageUserDTO;
import com.cc.booktalk.entity.dto.user.UserDTO;
import com.cc.booktalk.entity.dto.user.UserLoginDTO;
import com.cc.booktalk.entity.entity.user.User;
import com.cc.booktalk.entity.vo.PageUserVO;
import com.cc.booktalk.entity.vo.user.UserLoginVO;
import com.cc.booktalk.application.admin.service.UserAdminService;
import com.cc.booktalk.infrastructure.persistence.admin.mapper.UserAdminMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 用户登录账号信息表 服务实现类
 * </p>
 *
 * @author cc
 * @since 2025-07-14
 */
@Service
public class UserAdminServiceImpl extends ServiceImpl<UserAdminMapper, User> implements UserAdminService {


    @Resource
    private UserAdminMapper userAdminMapper;

    /**
     * 用户登录
     * @param userLoginDTO
     * @return
     */
    @Override
    public UserLoginVO login(UserLoginDTO userLoginDTO) {

        //判空操作通过@valid注解实现
        String username = userLoginDTO.getUsername();
        String password = userLoginDTO.getPassword();

        //1.检查用户是否存在
        LambdaQueryWrapper<User> query = Wrappers.lambdaQuery();
        query.eq(User::getUsername,username);
        User user = userAdminMapper.selectOne(query);
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
        userDTO.setId(user.getId());
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
     * 用户分页查询
     * @param pageUserDTO
     * @return
     */
    @Override
    public PageResult getPageUser(PageUserDTO pageUserDTO) {
        // 1. 启动分页
        PageHelper.startPage(pageUserDTO.getPageNum(), pageUserDTO.getPageSize());

        // 2. 构建查询条件
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(pageUserDTO.getUsername())) {
            wrapper.like(User::getUsername, pageUserDTO.getUsername());
        }

        // 3. 执行查询（必须是返回 List<User>）
        List<User> userList = userAdminMapper.selectList(wrapper);

        // PageHelper 会自动将结果封装成 Page 对象
        Page<User> pageInfo = (Page<User>) userList;

        // 4. 转换为 VO 列表
        List<PageUserVO> voList = userList.stream()
                .map(user -> ConvertUtils.convert(user, PageUserVO.class))
                .collect(Collectors.toList());

        // 5. 封装 PageResult
        return new PageResult<>(pageInfo.getTotal(), voList);
    }



}
