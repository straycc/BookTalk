package com.cc.booktalk.common.jwt;

import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTUtil;
import cn.hutool.core.date.DateUtil;
import com.cc.booktalk.common.exception.BaseException;
import com.cc.booktalk.interfaces.dto.user.UserDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class JwtUtil {

    private final byte[] secretKey;
    private final int expirationHours;

    public JwtUtil(JwtProperties properties) {
        String configuredSecret = properties.getSecret();
        if (configuredSecret == null || configuredSecret.isBlank()) {
            byte[] randomSecret = new byte[48];
            new SecureRandom().nextBytes(randomSecret);
            configuredSecret = Base64.getEncoder().encodeToString(randomSecret);
            log.warn("未配置 JWT_SECRET，已生成临时 JWT 密钥；应用重启后已有 Token 将失效");
        } else if (configuredSecret.length() < 32) {
            throw new IllegalStateException("JWT_SECRET 长度不能少于 32 个字符");
        }
        this.secretKey = configuredSecret.getBytes(StandardCharsets.UTF_8);
        this.expirationHours = properties.getExpirationHours();
    }

    public String generateToken(UserDTO userDTO) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", userDTO.getId());
        payload.put("username", userDTO.getUsername());
        payload.put("nickName", userDTO.getNickname());
        payload.put("avatar", userDTO.getAvatarUrl());
        payload.put("status", userDTO.getStatus());
        payload.put("role", userDTO.getRole());

        // xx小时后过期，放入秒级时间戳
        Date expireDate = DateUtil.offsetHour(new Date(), expirationHours);
        long expireTimestamp = expireDate.getTime() / 1000;
        payload.put("expire", expireTimestamp);

        return JWT.create()
                .addPayloads(payload)
                .setKey(secretKey)
                .sign();
    }
    // 验证并解析 Token，返回 JWT 对象
    public JWT verifyToken(String token) {
        JWT jwt = JWTUtil.parseToken(token).setKey(secretKey);
        if (!jwt.verify()) {
            throw new BaseException("Invalid token");
        }

        Object expireObj = jwt.getPayload("expire");
        long expireTimestamp;
        if (expireObj instanceof Number) {
            expireTimestamp = ((Number) expireObj).longValue();
        } else {
            // 如果是字符串，尝试转成数字
            expireTimestamp = Long.parseLong(expireObj.toString());
        }

        Date expire = new Date(expireTimestamp * 1000L);  // 秒转毫秒
        if (expire.before(new Date())) {
            throw new BaseException("Token expired");
        }

        return jwt;
    }

    //从 JWT 中提取用户信息，构造 UserDTO
    public UserDTO parseUserDTO(JWT jwt) {
        Object userIdObj = jwt.getPayload("userId");
        Object usernameObj = jwt.getPayload("username");
        Object statusObj = jwt.getPayload("status");
        Object roleObj = jwt.getPayload("role");
        Object avatarObj = jwt.getPayload("avatar");
        Object nickNameObj = jwt.getPayload("nickName");

        if (userIdObj == null || usernameObj == null || statusObj == null || roleObj == null) {
            throw new BaseException("Invalid token");
        }

        Long userId = Long.valueOf(userIdObj.toString());
        String username = usernameObj.toString();
        int status = Integer.parseInt(statusObj.toString());
        String role = roleObj.toString();
        String avatar = avatarObj == null ? "" : avatarObj.toString();
        String nickName = nickNameObj == null ? "" : nickNameObj.toString();
        if (status != 1) {
            throw new BaseException("账号已被禁用，请联系管理员");
        }
        return UserDTO.builder()
                .id(userId)
                .username(username)
                .nickname(nickName)
                .avatarUrl(avatar)
                .status(status)
                .role(role)
                .build();
    }
}

