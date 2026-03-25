package com.cc.booktalk.common.ai;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * AI 模块配置。
 */
@Data
@ConfigurationProperties(prefix = "app.ai")
public class AiProperties {

    /**
     * 是否启用真实模型调用。
     */
    private boolean enabled = false;

    /**
     * OpenAI 兼容接口地址。
     */
    private String baseUrl = "https://api.openai.com/v1";

    /**
     * 模型 API Key。
     */
    private String apiKey;

    /**
     * 默认模型名称。
     */
    private String model = "gpt-4o-mini";

    /**
     * 连接超时时间，毫秒。
     */
    private int connectTimeoutMillis = 5000;

    /**
     * 读取超时时间，毫秒。
     */
    private int readTimeoutMillis = 30000;

    /**
     * 会话中最多保留的消息条数。
     */
    private int maxSessionTurns = 12;

    /**
     * 会话缓存时长，小时。
     */
    private int sessionTtlHours = 24;
}
