package com.cc.booktalk.application.user.service.ai.impl;

import com.cc.booktalk.application.user.service.ai.AiPromptService;
import com.cc.booktalk.domain.ai.AiRecommendationContext;
import com.cc.booktalk.infrastructure.ai.model.LlmChatMessage;
import com.cc.booktalk.infrastructure.ai.model.LlmChatRequest;
import com.cc.booktalk.interfaces.vo.user.rec.PersonalizedRecVO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AI Prompt 组装服务。
 */
@Service
public class AiPromptServiceImpl implements AiPromptService {

    @Override
    public LlmChatRequest buildIntentRequest(String userInput, String conversationSummary) {
        String systemPrompt = "你是图书推荐意图解析器。"
                + "你只能根据用户问题提取结构化推荐条件，输出必须是 JSON。"
                + "字段固定为 intent,themes,preferredAuthors,preferredCategories,tone,difficulty,constraints,exclude,querySummary。"
                + "不要推荐书，不要输出 markdown，不要编造站外信息。";

        String userPrompt = "历史上下文：" + blankAsNone(conversationSummary)
                + "\n用户问题：" + userInput
                + "\n请输出 JSON。";

        return LlmChatRequest.builder()
                .messages(List.of(
                        LlmChatMessage.builder().role("system").content(systemPrompt).build(),
                        LlmChatMessage.builder().role("user").content(userPrompt).build()
                ))
                .temperature(0.1D)
                .maxTokens(400)
                .build();
    }

    @Override
    public LlmChatRequest buildAnswerRequest(AiRecommendationContext context) {
        String systemPrompt = "你是站内图书推荐助手。"
                + "你只能基于给出的站内候选书回答。"
                + "输出必须是 JSON，字段固定为 answer,bookReasons,followUpSuggestions。"
                + "bookReasons 的 key 使用 bookId，value 是一句推荐理由。"
                + "不能推荐候选之外的书，不能编造剧情和站外书单。";

        String books = context.getCandidateBooks().stream()
                .map(this::toBookLine)
                .collect(Collectors.joining("\n"));
        String userPrompt = "历史上下文：" + blankAsNone(context.getConversationSummary())
                + "\n用户问题：" + context.getUserInput()
                + "\n结构化意图：" + context.getParsedIntent()
                + "\n候选书：\n" + books
                + "\n请输出 JSON。";

        return LlmChatRequest.builder()
                .messages(List.of(
                        LlmChatMessage.builder().role("system").content(systemPrompt).build(),
                        LlmChatMessage.builder().role("user").content(userPrompt).build()
                ))
                .temperature(0.5D)
                .maxTokens(800)
                .build();
    }

    private String toBookLine(PersonalizedRecVO book) {
        List<String> parts = new ArrayList<>();
        parts.add("bookId=" + book.getBookId());
        parts.add("书名=" + blankAsNone(book.getBookTitle()));
        parts.add("作者=" + blankAsNone(book.getAuthor()));
        parts.add("推荐依据=" + blankAsNone(book.getReason()));
        parts.add("标签=" + listAsText(book.getMatchedTags()));
        parts.add("分类=" + listAsText(book.getMatchedCategories()));
        parts.add("置信度=" + book.getConfidence());
        return String.join("，", parts);
    }

    private String listAsText(List<String> values) {
        return values == null || values.isEmpty() ? "无" : String.join("、", values);
    }

    private String blankAsNone(String value) {
        return value == null || value.isBlank() ? "无" : value;
    }
}
