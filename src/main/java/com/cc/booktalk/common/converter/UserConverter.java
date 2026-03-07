package com.cc.booktalk.common.converter;

import com.cc.booktalk.interfaces.dto.user.UserDTO;
import com.cc.booktalk.domain.entity.user.User;
import com.cc.booktalk.domain.entity.user.UserInfo;
import com.cc.booktalk.interfaces.vo.user.user.UserLoginVO;
import com.cc.booktalk.interfaces.vo.user.user.UserVO;
import org.springframework.beans.BeanUtils;


public class UserConverter {


    public static UserDTO toUserDTO(User user,UserInfo userInfo){

        UserDTO userDTO = new UserDTO();
        if(user == null ){
            return userDTO;
        }

        BeanUtils.copyProperties(user, userDTO);
        if (userInfo != null) {
            userDTO.setNickname(userInfo.getNickname());
            userDTO.setAvatarUrl(userInfo.getAvatarUrl());
        }
        return userDTO;
    }

    public static UserLoginVO toUserLoginVO(User user, UserInfo userInfo, String token){
        UserLoginVO userLoginVO = new UserLoginVO();
        // 如果user为null，直接返回空对象
        if (user == null) {
            return userLoginVO;
        }
        BeanUtils.copyProperties(user, userLoginVO);
        // BeanUtils 不会自动把 user.id 映射到 userId，需要显式设置
        userLoginVO.setUserId(user.getId());
        // 如果userInfo不为null，设置昵称和头像
        if (userInfo != null) {
            userLoginVO.setNickname(userInfo.getNickname());
            userLoginVO.setAvatarUrl(userInfo.getAvatarUrl());
        }
        if(token != null){
            userLoginVO.setToken(token);
        }
        return userLoginVO;
    }


    public static UserVO toUserVO(User user,UserInfo userInfo){
        UserVO userVO = new UserVO();
        if(user == null){
            return userVO;
        }
        BeanUtils.copyProperties(user, userVO);
        // BeanUtils 不会自动把 user.id 映射到 userId，需要显式设置
        userVO.setUserId(user.getId());
        if (userInfo != null) {
            userVO.setNickname(userInfo.getNickname());
            userVO.setAvatarUrl(userInfo.getAvatarUrl());
            userVO.setExperience(userInfo.getExperience());
            userVO.setBackground(userInfo.getBackground());
            userVO.setGender(userInfo.getGender());
            userVO.setLevel(userInfo.getLevel());
            userVO.setSignature(userInfo.getSignature());
            userVO.setCreateTime(userInfo.getCreateTime());
            userVO.setUpdateTime(userInfo.getUpdateTime());
        }
        return userVO;
    }
}
