package com.cc.talkadmin;

import com.cc.talkcommon.oss.AliOssProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableConfigurationProperties(AliOssProperties.class)
@MapperScan("com.cc.talkadmin.mapper")
@ComponentScan(basePackages = {"com.cc.talkcommon", "com.cc.talkadmin", "com.cc.talkserver"})
public class TalkAdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(TalkAdminApplication.class, args);
    }

}
