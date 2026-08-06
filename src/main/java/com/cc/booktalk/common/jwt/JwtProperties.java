package com.cc.booktalk.common.jwt;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    /**
     * 生产环境必须通过 JWT_SECRET 设置。为空时应用会生成仅供本次运行使用的随机密钥。
     */
    private String secret;

    private int expirationHours = 24;
}
