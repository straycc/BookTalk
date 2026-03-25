package com.cc.booktalk.infrastructure.ai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 大模型响应结果。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LlmChatResponse {

    private String content;
}
