package com.cc.booktalk;

import com.cc.booktalk.common.oss.AliOssProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication(scanBasePackages = {//
        "com.cc.booktalk"
})
@EnableAsync
@EnableConfigurationProperties(AliOssProperties.class)
@MapperScan("com.cc.booktalk.infrastructure.persistence")

public class BookTalkApplication {

    public static void main(String[] args) {
        SpringApplication.run(BookTalkApplication.class, args);
    }

}
