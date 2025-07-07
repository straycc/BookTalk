package com.cc.talkcommon.interceptor;

import cn.hutool.http.HttpStatus;
import cn.hutool.jwt.JWT;
import com.cc.talkcommon.context.UserContext;
import com.cc.talkcommon.jwt.JwtUtil;
import com.cc.talkpojo.dto.UserDTO;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
 * 登录拦截器（拦截未登录请求）
 */

@Component
public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1. 从请求头中获取 token
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(HttpStatus.HTTP_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"message\":\"未登录：缺少Token\"}");
            return false;
        }

        String token = authHeader.substring(7);

        // 2. 校验 token
        try {
            JWT jwt = JwtUtil.verifyToken(token);
            UserDTO userDTO = JwtUtil.parseUserDTO(jwt);
            if (userDTO == null) {
                response.setStatus(HttpStatus.HTTP_UNAUTHORIZED);
                response.getWriter().write("{\"message\":\"Token无效\"}");
                return false;
            }

            // 3. 保存用户信息到 ThreadLocal
            UserContext.saveUser(userDTO);
        } catch (Exception e) {
            response.setStatus(HttpStatus.HTTP_UNAUTHORIZED);
            response.getWriter().write("{\"message\":\"Token校验失败\"}");
            return false;
        }

        // 4. 放行
        return true;

    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        UserContext.removeUser();
    }

}
