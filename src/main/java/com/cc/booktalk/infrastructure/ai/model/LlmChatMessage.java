package com.cc.booktalk.infrastructure.ai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 大模型消息体。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LlmChatMessage {

    private String role;

    private String content;
}
