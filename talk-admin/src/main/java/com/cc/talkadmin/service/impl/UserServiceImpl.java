package com.cc.talkadmin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cc.talkcommon.utils.ConvertUtils;
import com.cc.talkpojo.Result.PageResult;
import com.cc.talkpojo.dto.PageUserDTO;
import com.cc.talkpojo.entity.User;
import com.cc.talkadmin.mapper.UserMapper;
import com.cc.talkadmin.service.IUserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cc.talkpojo.vo.PageUserVO;
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
 * @since 2025-07-05
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {


    @Resource
    private UserMapper userMapper;


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
        List<User> userList = userMapper.selectList(wrapper);

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
