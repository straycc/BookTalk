package com.cc.booktalk.infrastructure.ai;

import com.cc.booktalk.infrastructure.ai.model.LlmChatRequest;
import com.cc.booktalk.infrastructure.ai.model.LlmChatResponse;

/**
 * 大模型客户端抽象。
 */
public interface LlmClient {

    /**
     * 是否可用。
     */
    boolean isAvailable();

    /**
     * 调用聊天接口。
     */
    LlmChatResponse chat(LlmChatRequest request);
}
