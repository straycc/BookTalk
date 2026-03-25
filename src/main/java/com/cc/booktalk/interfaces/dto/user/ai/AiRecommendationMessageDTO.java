package com.cc.booktalk.interfaces.dto.user.ai;

import lombok.Data;

/**
 * AI WebSocket 入站消息。
 */
@Data
public class AiRecommendationMessageDTO {

    private String type;

    private String sessionId;

    private String content;
}
