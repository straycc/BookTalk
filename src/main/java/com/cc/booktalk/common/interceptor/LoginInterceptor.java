package com.cc.booktalk.common.interceptor;

import cn.hutool.http.HttpStatus;
import cn.hutool.jwt.JWT;
import com.cc.booktalk.common.context.UserContext;
import com.cc.booktalk.common.jwt.JwtUtil;
import com.cc.booktalk.interfaces.dto.user.UserDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
 * 登录拦截器（拦截未登录请求）
 */

@Component
@Slf4j
@RequiredArgsConstructor
public class LoginInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // CORS 预检请求直接放行，避免浏览器跨域请求被拦截
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // WebSocket 握手请求放行（标准 Upgrade 头）
        String upgrade = request.getHeader("Upgrade");
        if (upgrade != null && "websocket".equalsIgnoreCase(upgrade)) {
            return true;
        }

        // WebSocket 握手请求放行（路径中包含 /ws/）
        String requestUri = request.getRequestURI();
        if (requestUri != null && requestUri.contains("/ws/")) {
            return true;
        }

        // 1. 从请求头中获取 token
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(HttpStatus.HTTP_UNAUTHORIZED);
            response.setCharacterEncoding("UTF-8");
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"message\":\"未登录：缺少Token\"}");
            return false;
        }

        String token = authHeader.substring(7);

        // 2. 校验 token
        try {
            JWT jwt = jwtUtil.verifyToken(token);
            UserDTO userDTO = jwtUtil.parseUserDTO(jwt);
            if (userDTO == null) {
                response.setStatus(HttpStatus.HTTP_UNAUTHORIZED);
                response.getWriter().write("{\"message\":\"Token无效\"}");
                return false;
            }
            if (requestUri != null && requestUri.startsWith("/admin/")
                    && !"admin".equalsIgnoreCase(userDTO.getRole())) {
                response.setStatus(HttpStatus.HTTP_FORBIDDEN);
                response.setCharacterEncoding("UTF-8");
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":403,\"msg\":\"没有管理员权限\",\"data\":null}");
                return false;
            }

            // 3. 保存用户信息到 ThreadLocal
            UserContext.saveUser(userDTO);
        } catch (Exception e) {
            response.setStatus(HttpStatus.HTTP_UNAUTHORIZED);
            response.setCharacterEncoding("UTF-8");
            response.setContentType("application/json;charset=UTF-8");

            log.debug("Token 校验失败: {}", e.getMessage());
            response.getWriter().write("{\"code\":401,\"msg\":\"登录状态无效或已过期\",\"data\":null}");
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
