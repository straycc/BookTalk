package com.cc.booktalk;

import com.cc.booktalk.common.oss.AliOssProperties;
import com.cc.booktalk.common.ai.AiProperties;
import com.cc.booktalk.common.jwt.JwtProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@EnableConfigurationProperties({AliOssProperties.class, AiProperties.class, JwtProperties.class})
@MapperScan("com.cc.booktalk.infrastructure.persistence")

public class BookTalkApplication {

    public static void main(String[] args) {
        SpringApplication.run(BookTalkApplication.class, args);
    }

}
