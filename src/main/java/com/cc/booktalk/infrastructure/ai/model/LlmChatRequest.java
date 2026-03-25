package com.cc.booktalk.infrastructure.ai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 大模型请求参数。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LlmChatRequest {

    private String model;

    @Builder.Default
    private List<LlmChatMessage> messages = new ArrayList<>();

    @Builder.Default
    private double temperature = 0.3D;

    @Builder.Default
    private int maxTokens = 800;
}
