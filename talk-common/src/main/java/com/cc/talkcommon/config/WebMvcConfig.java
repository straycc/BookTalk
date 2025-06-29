package com.cc.talkcommon.config;

import com.cc.talkcommon.Json.JacksonObjectMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


@Configuration
public class WebMvcConfig implements WebMvcConfigurer {


    /**
     * 注册JacksonObjectMapper
     * @return
     */
    @Bean
    public ObjectMapper objectMapper(){
        return new JacksonObjectMapper();
    }
}
