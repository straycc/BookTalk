package com.cc.talkuser;

import com.cc.talkcommon.oss.AliOssProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@EnableConfigurationProperties(AliOssProperties.class)
@MapperScan("com.cc.talkserver.user.mapper")
@ComponentScan(basePackages = {
        "com.cc.talkcommon",
        "com.cc.talkuser",
        "com.cc.talkserver.user",
        "com.cc.talkserver.user.service",
        "com.cc.talkserver.listener",     // 监听器包
        "com.cc.talkserver.config",
        "com.cc.talkserver.schedule",
        "com.cc.talkserver.websocket"
})

public class TalkUserApplication {

    public static void main(String[] args) {
        SpringApplication.run(TalkUserApplication.class, args);
    }

}
