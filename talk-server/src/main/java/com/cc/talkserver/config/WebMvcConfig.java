package com.cc.talkserver.config;

import com.cc.talkcommon.Json.JacksonObjectMapper;
import com.cc.talkcommon.interceptor.LoginInterceptor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;


@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Resource
    private LoginInterceptor loginInterceptor;


    /**
     * 注册JacksonObjectMapper
     * @return
     */
    @Bean
    public ObjectMapper objectMapper(){
        return new JacksonObjectMapper();
    }

    /**
     * 注册登录拦截器
     * @return
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/**")  // 拦截所有请求
                .excludePathPatterns(
                        "/user/login",
                        "/user/register",
                        "/admin/login/",
                        "/doc.html",
                        "/swagger-ui.html",
                        "/swagger-resources/**",
                        "/webjars/**",
                        "/v2/api-docs",
                        "/v3/api-docs" // 新版本swagger路径
                ).order(1);
    }

    /**
     * 解决啊前后端跨域问题
     * @return
     */
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOriginPatterns("http://localhost:5173") // 允许前端访问地址（不要用 *，否则不能配合 allowCredentials）
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true) // 允许携带 Cookie/Token
                        .maxAge(3600);
            }
        };
    }
}
