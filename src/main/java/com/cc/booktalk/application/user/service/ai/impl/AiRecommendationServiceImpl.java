package com.cc.booktalk.application.user.service.ai.impl;

import com.cc.booktalk.application.user.service.ai.AiConversationService;
import com.cc.booktalk.application.user.service.ai.AiPromptService;
import com.cc.booktalk.application.user.service.ai.AiRecommendationService;
import com.cc.booktalk.application.user.service.book.BookUserService;
import com.cc.booktalk.application.user.service.recommendation.RecommendationService;
import com.cc.booktalk.common.exception.BaseException;
import com.cc.booktalk.common.result.PageResult;
import com.cc.booktalk.domain.ai.AiConversationSession;
import com.cc.booktalk.domain.ai.AiConversationTurn;
import com.cc.booktalk.domain.ai.AiParsedIntent;
import com.cc.booktalk.domain.ai.AiRecommendationContext;
import com.cc.booktalk.domain.ai.AiRecommendedBook;
import com.cc.booktalk.infrastructure.ai.LlmClient;
import com.cc.booktalk.infrastructure.ai.model.LlmChatRequest;
import com.cc.booktalk.infrastructure.ai.model.LlmChatResponse;
import com.cc.booktalk.interfaces.dto.user.search.PageSearchDTO;
import com.cc.booktalk.interfaces.dto.user.book.BookShowDTO;
import com.cc.booktalk.interfaces.vo.user.ai.AiRecommendedBookVO;
import com.cc.booktalk.interfaces.vo.user.ai.AiRecommendationResponseVO;
import com.cc.booktalk.interfaces.vo.user.rec.PersonalizedRecVO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * AI 推荐主服务实现。
 */
@Service
public class AiRecommendationServiceImpl implements AiRecommendationService {

    private static final int CANDIDATE_LIMIT = 20;
    private static final int RETURN_LIMIT = 5;

    @Resource
    private AiConversationService aiConversationService;

    @Resource
    private AiPromptService aiPromptService;

    @Resource
    private RecommendationService recommendationService;

    @Resource
    private BookUserService bookUserService;

    @Resource
    private LlmClient llmClient;

    @Resource
    private ObjectMapper objectMapper;

    @Override
    public AiRecommendationResponseVO ask(Long userId, String sessionId, String userInput, String messageType) {
        if (userId == null) {
            throw new BaseException("用户未登录");
        }
        if (userInput == null || userInput.isBlank()) {
            throw new BaseException("请输入想看的书籍方向");
        }

        AiConversationSession session = aiConversationService.loadSession(userId, sessionId);
        String resolvedSessionId = session.getSessionId();
        String conversationSummary = aiConversationService.summarizeConversation(session);

        AiParsedIntent parsedIntent = parseIntent(userInput, conversationSummary, session);
        List<PersonalizedRecVO> candidates = resolveCandidates(userId, userInput, parsedIntent, session, messageType);

        AiRecommendationContext context = AiRecommendationContext.builder()
                .userId(userId)
                .sessionId(resolvedSessionId)
                .userInput(userInput)
                .messageType(messageType)
                .conversationSummary(conversationSummary)
                .parsedIntent(parsedIntent)
                .candidateBooks(candidates)
                .build();

        AiRecommendationResponseVO response = buildAnswer(context);

        aiConversationService.appendTurn(resolvedSessionId, userId, AiConversationTurn.builder()
                .role("user")
                .content(userInput)
                .createdAt(LocalDateTime.now())
                .build());

        aiConversationService.appendTurn(resolvedSessionId, userId, AiConversationTurn.builder()
                .role("assistant")
                .content(response.getAnswer())
                .createdAt(LocalDateTime.now())
                .metadata(Map.of("bookCount", response.getBooks().size()))
                .build());

        aiConversationService.updateContext(
                resolvedSessionId,
                userId,
                parsedIntent,
                response.getBooks().stream().map(this::toDomainBook).collect(Collectors.toList())
        );
        response.setSessionId(resolvedSessionId);
        return response;
    }

    @Override
    public AiRecommendationResponseVO reset(Long userId, String sessionId) {
        if (sessionId != null && !sessionId.isBlank()) {
            aiConversationService.resetSession(sessionId, userId);
        }
        return AiRecommendationResponseVO.builder()
                .type("AI_SESSION_RESET")
                .sessionId(sessionId)
                .answer("会话已重置，你可以重新告诉我想看什么样的书。")
                .followUpSuggestions(List.of("想看轻松一点的小说", "给我推荐能引发讨论的书", "我想看女性作者作品"))
                .build();
    }

    private AiParsedIntent parseIntent(String userInput, String conversationSummary, AiConversationSession session) {
        if (llmClient.isAvailable()) {
            try {
                LlmChatRequest request = aiPromptService.buildIntentRequest(userInput, conversationSummary);
                LlmChatResponse response = llmClient.chat(request);
                AiParsedIntent intent = parseIntentJson(response.getContent());
                if (intent != null) {
                    if (intent.getQuerySummary() == null || intent.getQuerySummary().isBlank()) {
                        intent.setQuerySummary(userInput);
                    }
                    mergeSessionIntent(intent, session);
                    return intent;
                }
            } catch (Exception ignored) {
                // 真实模型不可用时降级到规则解析
            }
        }
        AiParsedIntent fallback = heuristicIntent(userInput);
        mergeSessionIntent(fallback, session);
        return fallback;
    }

    private void mergeSessionIntent(AiParsedIntent current, AiConversationSession session) {
        if (session == null || session.getLastIntent() == null) {
            return;
        }
        AiParsedIntent last = session.getLastIntent();
        if (current.getThemes().isEmpty()) {
            current.setThemes(new ArrayList<>(last.getThemes()));
        }
        if (current.getPreferredAuthors().isEmpty()) {
            current.setPreferredAuthors(new ArrayList<>(last.getPreferredAuthors()));
        }
        if (current.getPreferredCategories().isEmpty()) {
            current.setPreferredCategories(new ArrayList<>(last.getPreferredCategories()));
        }
        if ((current.getTone() == null || current.getTone().isBlank()) && last.getTone() != null) {
            current.setTone(last.getTone());
        }
        if ((current.getDifficulty() == null || current.getDifficulty().isBlank()) && last.getDifficulty() != null) {
            current.setDifficulty(last.getDifficulty());
        }
    }

    private AiParsedIntent parseIntentJson(String content) {
        try {
            String json = extractJson(content);
            JsonNode root = objectMapper.readTree(json);
            return AiParsedIntent.builder()
                    .intent(root.path("intent").asText("RECOMMEND_BOOKS"))
                    .themes(readStringList(root, "themes"))
                    .preferredAuthors(readStringList(root, "preferredAuthors"))
                    .preferredCategories(readStringList(root, "preferredCategories"))
                    .constraints(readStringList(root, "constraints"))
                    .exclude(readStringList(root, "exclude"))
                    .tone(root.path("tone").asText(null))
                    .difficulty(root.path("difficulty").asText(null))
                    .querySummary(root.path("querySummary").asText(null))
                    .build();
        } catch (Exception e) {
            return null;
        }
    }

    private List<PersonalizedRecVO> resolveCandidates(Long userId, String userInput, AiParsedIntent intent,
                                                      AiConversationSession session, String messageType) {
        Map<Long, PersonalizedRecVO> merged = new LinkedHashMap<>();

        if (isFollowUpSelection(messageType, userInput) && session != null && session.getLastBooks() != null && !session.getLastBooks().isEmpty()) {
            for (AiRecommendedBook lastBook : session.getLastBooks()) {
                merged.put(lastBook.getBookId(), PersonalizedRecVO.builder()
                        .bookId(lastBook.getBookId())
                        .bookTitle(lastBook.getBookTitle())
                        .author(lastBook.getAuthor())
                        .bookCover(lastBook.getBookCover())
                        .reason(lastBook.getReason())
                        .confidence(lastBook.getConfidence())
                        .score(lastBook.getScore())
                        .build());
            }
        } else {
            mergeRecommendations(merged, recommendationService.getPersonalizedRecommendations(userId, CANDIDATE_LIMIT));
            mergeRecommendations(merged, recommendationService.getHotRecommendations(CANDIDATE_LIMIT / 2));
            mergeSearchResults(merged, searchBooks(userInput, intent));
        }

        return merged.values().stream()
                .peek(item -> item.setScore(scoreCandidate(item, intent, userInput)))
                .sorted(Comparator.comparing(PersonalizedRecVO::getScore, Comparator.nullsLast(Double::compareTo)).reversed())
                .limit(RETURN_LIMIT)
                .collect(Collectors.toList());
    }

    private void mergeRecommendations(Map<Long, PersonalizedRecVO> merged, List<PersonalizedRecVO> recommendations) {
        if (recommendations == null) {
            return;
        }
        for (PersonalizedRecVO recommendation : recommendations) {
            if (recommendation.getBookId() == null) {
                continue;
            }
            merged.putIfAbsent(recommendation.getBookId(), recommendation);
        }
    }

    private void mergeSearchResults(Map<Long, PersonalizedRecVO> merged, List<PersonalizedRecVO> results) {
        if (results == null) {
            return;
        }
        for (PersonalizedRecVO item : results) {
            if (item.getBookId() == null) {
                continue;
            }
            merged.merge(item.getBookId(), item, (left, right) -> {
                if (left.getReason() == null || left.getReason().isBlank()) {
                    left.setReason(right.getReason());
                }
                left.setScore(Math.max(defaultScore(left), defaultScore(right)));
                return left;
            });
        }
    }

    private List<PersonalizedRecVO> searchBooks(String userInput, AiParsedIntent intent) {
        Set<String> keywords = new java.util.LinkedHashSet<>();
        keywords.add(userInput);
        keywords.addAll(intent.getPreferredAuthors());
        keywords.addAll(intent.getThemes());

        List<PersonalizedRecVO> results = new ArrayList<>();
        for (String keyword : keywords) {
            if (keyword == null || keyword.isBlank() || keyword.length() < 2) {
                continue;
            }
            PageSearchDTO searchDTO = new PageSearchDTO();
            searchDTO.setKeyword(keyword);
            searchDTO.setPageNum(1);
            searchDTO.setPageSize(5);
            try {
                PageResult<BookShowDTO> pageResult = bookUserService.getSearchPage(searchDTO);
                if (pageResult == null || pageResult.getRecords() == null) {
                    continue;
                }
                for (BookShowDTO record : pageResult.getRecords()) {
                    results.add(PersonalizedRecVO.builder()
                            .bookId(record.getId())
                            .bookTitle(record.getTitle())
                            .author(record.getAuthor())
                            .bookCover(record.getCoverUrl())
                            .score(70D)
                            .reason("根据你的问题从站内书库检索到相关图书")
                            .reasonCodes(List.of("SEARCH_MATCH"))
                            .matchedTags(List.of(keyword))
                            .sourceStrategies(List.of("SEARCH_MATCH"))
                            .confidence(0.55D)
                            .algorithmType("AI_SEARCH")
                            .build());
                }
            } catch (Exception ignored) {
                // 搜索失败时不阻断 AI 对话主链路
            }
        }
        return results;
    }

    private double scoreCandidate(PersonalizedRecVO candidate, AiParsedIntent intent, String userInput) {
        double score = defaultScore(candidate);
        String title = normalize(candidate.getBookTitle());
        String author = normalize(candidate.getAuthor());
        String reason = normalize(candidate.getReason());
        String related = normalize(candidate.getRelatedInterests());
        String input = normalize(userInput);

        if (!input.isBlank() && (title.contains(input) || author.contains(input))) {
            score += 40D;
        }
        for (String theme : intent.getThemes()) {
            String normalizedTheme = normalize(theme);
            if (normalizedTheme.isBlank()) {
                continue;
            }
            if (title.contains(normalizedTheme) || reason.contains(normalizedTheme) || related.contains(normalizedTheme)) {
                score += 18D;
            }
        }
        for (String authorKeyword : intent.getPreferredAuthors()) {
            String normalizedAuthor = normalize(authorKeyword);
            if (!normalizedAuthor.isBlank() && author.contains(normalizedAuthor)) {
                score += 30D;
            }
        }
        for (String excluded : intent.getExclude()) {
            String normalizedExcluded = normalize(excluded);
            if (!normalizedExcluded.isBlank() && (title.contains(normalizedExcluded) || author.contains(normalizedExcluded))) {
                score -= 25D;
            }
        }
        return score;
    }

    private double defaultScore(PersonalizedRecVO candidate) {
        return candidate.getScore() == null ? 60D : candidate.getScore();
    }

    private AiRecommendationResponseVO buildAnswer(AiRecommendationContext context) {
        if (context.getCandidateBooks().isEmpty()) {
            return AiRecommendationResponseVO.builder()
                    .type("AI_RECOMMENDATION_RESULT")
                    .answer("当前站内书库里没有找到足够匹配的书，你可以换一个更明确的方向继续问我。")
                    .followUpSuggestions(List.of("想看更轻松一点的", "推荐更成熟一点的奇幻", "只要女性作者"))
                    .books(List.of())
                    .build();
        }

        Map<Long, String> aiBookReasons = new HashMap<>();
        List<String> followUps = new ArrayList<>();
        String answer = null;

        if (llmClient.isAvailable()) {
            try {
                LlmChatRequest request = aiPromptService.buildAnswerRequest(context);
                LlmChatResponse response = llmClient.chat(request);
                JsonNode root = objectMapper.readTree(extractJson(response.getContent()));
                answer = root.path("answer").asText(null);
                JsonNode reasons = root.path("bookReasons");
                if (reasons.isObject()) {
                    reasons.fields().forEachRemaining(entry -> {
                        try {
                            aiBookReasons.put(Long.parseLong(entry.getKey()), entry.getValue().asText());
                        } catch (NumberFormatException ignored) {
                            // 忽略非法 bookId
                        }
                    });
                }
                followUps = readStringList(root, "followUpSuggestions");
            } catch (Exception ignored) {
                // 调用失败时走规则兜底
            }
        }

        if (answer == null || answer.isBlank()) {
            answer = fallbackAnswer(context.getParsedIntent(), context.getCandidateBooks());
        }
        if (followUps.isEmpty()) {
            followUps = fallbackFollowUps(context.getParsedIntent());
        }

        List<AiRecommendedBookVO> books = context.getCandidateBooks().stream()
                .map(book -> AiRecommendedBookVO.builder()
                        .bookId(book.getBookId())
                        .bookTitle(book.getBookTitle())
                        .author(book.getAuthor())
                        .bookCover(book.getBookCover())
                        .reason(resolveBookReason(book, aiBookReasons))
                        .confidence(book.getConfidence())
                        .score(book.getScore())
                        .build())
                .collect(Collectors.toList());

        return AiRecommendationResponseVO.builder()
                .type("AI_RECOMMENDATION_RESULT")
                .answer(answer)
                .books(books)
                .followUpSuggestions(followUps)
                .build();
    }

    private String resolveBookReason(PersonalizedRecVO book, Map<Long, String> aiBookReasons) {
        String aiReason = aiBookReasons.get(book.getBookId());
        if (aiReason != null && !aiReason.isBlank()) {
            return aiReason;
        }
        if (book.getReason() != null && !book.getReason().isBlank()) {
            return book.getReason();
        }
        return "这本书与当前问题的方向比较接近。";
    }

    private String fallbackAnswer(AiParsedIntent intent, List<PersonalizedRecVO> books) {
        String theme = intent.getThemes().isEmpty() ? "当前方向" : String.join("、", intent.getThemes());
        String names = books.stream().map(PersonalizedRecVO::getBookTitle).collect(Collectors.joining("、"));
        return "我先基于站内书库和你当前的需求方向，挑出了这些更值得先聊的书：" + names + "。"
                + "它们整体更贴近“" + theme + "”这个方向。";
    }

    private List<String> fallbackFollowUps(AiParsedIntent intent) {
        List<String> suggestions = new ArrayList<>();
        suggestions.add("换成更治愈一点的");
        suggestions.add("不要太长");
        if (!intent.getPreferredAuthors().isEmpty()) {
            suggestions.add("同作者还有什么可读的");
        } else {
            suggestions.add("最好是女性作者");
        }
        return suggestions.stream().limit(3).collect(Collectors.toList());
    }

    private AiRecommendedBook toDomainBook(AiRecommendedBookVO book) {
        return AiRecommendedBook.builder()
                .bookId(book.getBookId())
                .bookTitle(book.getBookTitle())
                .author(book.getAuthor())
                .bookCover(book.getBookCover())
                .reason(book.getReason())
                .confidence(book.getConfidence())
                .score(book.getScore())
                .build();
    }

    private boolean isFollowUpSelection(String messageType, String userInput) {
        if ("FOLLOW_UP".equalsIgnoreCase(messageType)) {
            return true;
        }
        String normalized = normalize(userInput);
        return normalized.contains("这几本") || normalized.contains("哪本") || normalized.contains("上一轮");
    }

    private AiParsedIntent heuristicIntent(String userInput) {
        String normalized = userInput == null ? "" : userInput.trim();
        List<String> themes = new ArrayList<>();
        List<String> constraints = new ArrayList<>();
        List<String> excludes = new ArrayList<>();
        List<String> authors = new ArrayList<>();

        if (normalized.contains("奇幻")) themes.add("奇幻");
        if (normalized.contains("心理")) themes.add("心理");
        if (normalized.contains("成长")) themes.add("成长");
        if (normalized.contains("历史")) themes.add("历史");
        if (normalized.contains("推理")) themes.add("推理");
        if (normalized.contains("治愈")) themes.add("治愈");
        if (normalized.contains("哈利波特")) themes.add("魔法成长");

        if (normalized.contains("不要太长") || normalized.contains("短一点")) constraints.add("篇幅短");
        if (normalized.contains("女性作者")) constraints.add("女性作者");
        if (normalized.contains("入门")) constraints.add("适合入门");
        if (normalized.contains("不要太沉重")) excludes.add("沉重");

        if (normalized.toLowerCase(Locale.ROOT).contains("罗琳") || normalized.contains("J.K.")) {
            authors.add("J.K.罗琳");
        }

        return AiParsedIntent.builder()
                .intent("RECOMMEND_BOOKS")
                .themes(themes)
                .preferredAuthors(authors)
                .preferredCategories(new ArrayList<>())
                .constraints(constraints)
                .exclude(excludes)
                .querySummary(normalized)
                .build();
    }

    private List<String> readStringList(JsonNode root, String fieldName) {
        JsonNode node = root.path(fieldName);
        if (!node.isArray()) {
            return new ArrayList<>();
        }
        return objectMapper.convertValue(node, new TypeReference<List<String>>() {});
    }

    private String extractJson(String content) {
        int start = content.indexOf('{');
        int end = content.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return content.substring(start, end + 1);
        }
        throw new BaseException("AI 响应不是合法 JSON");
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replace(" ", "");
    }
}
