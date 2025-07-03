package com.cc.talkserver.config;


import com.github.xiaoymin.knife4j.spring.annotations.EnableKnife4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableKnife4j
public class Knife4jConfig {
    @Bean
    public Docket createRestApiUser() {
        ApiInfo apiInfo = new ApiInfoBuilder()
                .title("BookTalk接口文档")
                .version("1.0")
                .description("书语接口文档")
                .build();
        return new Docket(DocumentationType.SWAGGER_2)
                .enable(true)
                .groupName("用户端相关接口")
                .apiInfo(apiInfo)
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.cc.talkuser.controller")) // 指定你自己的 controller 包路径
                .paths(PathSelectors.any())
                .build();
    }

    @Bean
    public Docket createRestApiAdmin() {
        ApiInfo apiInfo = new ApiInfoBuilder()
                .title("BookTalk接口文档")
                .version("1.0")
                .description("书语接口文档")
                .build();
        return new Docket(DocumentationType.SWAGGER_2)
                .enable(true)
                .groupName("管理端相关接口")
                .apiInfo(apiInfo)
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.cc.talkadmin.controller")) // 指定你自己的 controller 包路径
                .paths(PathSelectors.any())
                .build();
    }
}
