package com.cc.talkcommon.jwt;

import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTUtil;
import cn.hutool.core.date.DateUtil;
import com.cc.talkcommon.exception.BaseException;
import com.cc.talkpojo.dto.UserDTO;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class JwtUtil {

    private static final String SECRET_KEY = "my-secret-123456";

    // 生成 Token
    public static String generateToken(UserDTO userDTO) {
        Map<String, Object> payload = new HashMap<>();
        //载荷
        payload.put("userId", userDTO.getId());
        payload.put("userName", userDTO.getUsername());
        payload.put("status", userDTO.getStatus());
        //过期时间
        payload.put("expire", DateUtil.offsetHour(new Date(), 2)); // 2小时过期

        return JWT.create()
                .addPayloads(payload)
                .setKey(SECRET_KEY.getBytes())
                .sign();
    }

    // 验证并解析 Token，返回 JWT 对象
    public static JWT verifyToken(String token) {
        JWT jwt = JWTUtil.parseToken(token).setKey(SECRET_KEY.getBytes());
        if (!jwt.verify()) {
            throw new BaseException("Invalid token");
        }

        Date expire = DateUtil.parse(jwt.getPayload("expire").toString());
        if (expire.before(new Date())) {
            throw new BaseException("Token expired");
        }
        return jwt;
    }

    //从 JWT 中提取用户信息，构造 UserDTO
    public static UserDTO parseUserDTO(JWT jwt) {
        Long userId = Long.valueOf(jwt.getPayload("userId").toString());
        String username = jwt.getPayload("username").toString();
        int status = Integer.parseInt(jwt.getPayload("status").toString());
        if (status != 1) {
            throw new BaseException("账号已被禁用，请联系管理员");
        }
        return UserDTO.builder()
                .id(userId)
                .username(username)
                .status(status)
                .build();
    }
}

