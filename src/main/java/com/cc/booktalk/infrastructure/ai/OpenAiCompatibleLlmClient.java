package com.cc.booktalk.infrastructure.ai;

import com.cc.booktalk.common.ai.AiProperties;
import com.cc.booktalk.common.exception.BaseException;
import com.cc.booktalk.infrastructure.ai.model.LlmChatMessage;
import com.cc.booktalk.infrastructure.ai.model.LlmChatRequest;
import com.cc.booktalk.infrastructure.ai.model.LlmChatResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * OpenAI 兼容协议客户端。
 */
@Component
public class OpenAiCompatibleLlmClient implements LlmClient {

    @Resource
    private AiProperties aiProperties;

    @Resource
    private ObjectMapper objectMapper;

    @Override
    public boolean isAvailable() {
        return aiProperties.isEnabled()
                && aiProperties.getApiKey() != null
                && !aiProperties.getApiKey().isBlank()
                && aiProperties.getBaseUrl() != null
                && !aiProperties.getBaseUrl().isBlank();
    }

    @Override
    public LlmChatResponse chat(LlmChatRequest request) {
        if (!isAvailable()) {
            throw new BaseException("AI 模型未配置，请先设置 app.ai 配置");
        }
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofMillis(aiProperties.getConnectTimeoutMillis()))
                    .build();

            Map<String, Object> payload = new HashMap<>();
            payload.put("model", request.getModel() == null || request.getModel().isBlank()
                    ? aiProperties.getModel() : request.getModel());
            payload.put("temperature", request.getTemperature());
            payload.put("max_tokens", request.getMaxTokens());
            payload.put("messages", toMessages(request.getMessages()));

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(normalizeBaseUrl(aiProperties.getBaseUrl()) + "/chat/completions"))
                    .timeout(Duration.ofMillis(aiProperties.getReadTimeoutMillis()))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + aiProperties.getApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                    .build();

            HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BaseException("AI 模型调用失败: HTTP " + response.statusCode());
            }
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode choices = root.path("choices");
            if (!choices.isArray() || choices.isEmpty()) {
                throw new BaseException("AI 模型返回格式不合法");
            }
            String content = choices.get(0).path("message").path("content").asText("");
            if (content.isBlank()) {
                throw new BaseException("AI 模型返回为空");
            }
            return LlmChatResponse.builder().content(content).build();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BaseException("AI 模型调用异常: " + e.getMessage());
        } catch (IOException e) {
            throw new BaseException("AI 模型调用异常: " + e.getMessage());
        }
    }

    private String normalizeBaseUrl(String baseUrl) {
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    private List<Map<String, String>> toMessages(List<LlmChatMessage> messages) {
        return messages.stream().map(message -> {
            Map<String, String> item = new HashMap<>();
            item.put("role", message.getRole());
            item.put("content", message.getContent());
            return item;
        }).collect(Collectors.toList());
    }
}
