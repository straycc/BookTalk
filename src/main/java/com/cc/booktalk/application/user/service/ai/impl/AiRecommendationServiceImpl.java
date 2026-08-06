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
import com.cc.booktalk.domain.recommendation.model.RecommendationBookCandidate;
import com.cc.booktalk.infrastructure.ai.LlmClient;
import com.cc.booktalk.infrastructure.ai.model.LlmChatRequest;
import com.cc.booktalk.infrastructure.ai.model.LlmChatResponse;
import com.cc.booktalk.infrastructure.persistence.user.mapper.recommendation.UserInterestTagMapper;
import com.cc.booktalk.infrastructure.persistence.user.mapper.book.BookUserMapper;
import com.cc.booktalk.interfaces.dto.user.book.BookShowDTO;
import com.cc.booktalk.interfaces.dto.user.search.PageSearchDTO;
import com.cc.booktalk.interfaces.vo.user.ai.AiRecommendedBookVO;
import com.cc.booktalk.interfaces.vo.user.ai.AiRecommendationResponseVO;
import com.cc.booktalk.interfaces.vo.user.rec.PersonalizedRecVO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * AI 推荐主服务实现。
 */
@Service
@Slf4j
public class AiRecommendationServiceImpl implements AiRecommendationService {

    private static final int ROUGH_RECALL_LIMIT = 100;
    private static final int RERANK_KEEP_LIMIT = 30;
    private static final int RETURN_LIMIT = 5;

    private static final int INTENT_ROUTE_TARGET = 21;
    private static final int PROFILE_ROUTE_TARGET = 6;
    private static final int HOT_ROUTE_TARGET = 3;

    private static final String PHASE_INIT = "INIT";
    private static final String PHASE_NEED_CLARIFY = "NEED_CLARIFY";
    private static final String PHASE_INTENT_READY = "INTENT_READY";
    private static final String PHASE_RECOMMENDED = "RECOMMENDED";

    @Resource
    private AiConversationService aiConversationService;

    @Resource
    private AiPromptService aiPromptService;

    @Resource
    private RecommendationService recommendationService;

    @Resource
    private BookUserService bookUserService;

    @Resource
    private UserInterestTagMapper userInterestTagMapper;

    @Resource
    private BookUserMapper bookUserMapper;

    @Resource
    private LlmClient llmClient;

    @Resource
    private ObjectMapper objectMapper;

    @Override
    public AiRecommendationResponseVO ask(Long userId, String sessionId, String userInput, String messageType) {
        if (userId == null) {
            throw new BaseException("用户未登录");
        }

        String normalizedInput = userInput == null ? "" : userInput.trim();
        AiConversationSession session = aiConversationService.loadSession(userId, sessionId);
        String resolvedSessionId = session.getSessionId();
        String conversationSummary = aiConversationService.summarizeConversation(session);

        if (isInitRecommendationRequest(messageType, normalizedInput)) {
            return handleInitRecommendation(userId, resolvedSessionId, session, messageType);
        }

        AiParsedIntent parsedIntent = parseIntent(normalizedInput, conversationSummary, session);
        log.info("AI意图解析: userId={}, sessionId={}, messageType={}, input={}, intent={}, themes={}, authors={}, categories={}, constraints={}, exclude={}, confidence={}, needClarify={}",
                userId, resolvedSessionId, messageType, normalizedInput,
                parsedIntent.getIntent(), parsedIntent.getThemes(), parsedIntent.getPreferredAuthors(),
                parsedIntent.getPreferredCategories(), parsedIntent.getConstraints(), parsedIntent.getExclude(),
                parsedIntent.getIntentConfidence(), parsedIntent.getNeedClarify());
        boolean shouldClarify = needsClarify(parsedIntent, normalizedInput, session);
        log.info("AI追问判定: userId={}, sessionId={}, shouldClarify={}, hasRecallSignals={}, phase={}, modelNeedClarify={}",
                userId, resolvedSessionId, shouldClarify,
                hasRecallSignals(parsedIntent, normalizedInput),
                session.getRecommendationPhase(), parsedIntent.getNeedClarify());
        if (shouldClarify) {
            return handleClarifyQuestion(userId, resolvedSessionId, normalizedInput, parsedIntent);
        }

        boolean newBatch = isNewBatchRequest(messageType, normalizedInput);
        CandidateResolveResult candidateResult = resolveCandidates(userId, normalizedInput, parsedIntent, session, messageType, newBatch);
        log.info("AI候选结果: userId={}, sessionId={}, messageType={}, newBatch={}, finalBooks={}, cachedCandidates={}",
                userId, resolvedSessionId, messageType, newBatch,
                candidateResult.finalBooks.size(), candidateResult.cachedCandidateBookIds.size());

        if (candidateResult.finalBooks.isEmpty()) {
            return buildEmptyResultResponse(resolvedSessionId, newBatch);
        }

        aiConversationService.updateRecommendationState(
                resolvedSessionId,
                userId,
                PHASE_INTENT_READY,
                buildIntentDigest(parsedIntent),
                null,
                candidateResult.cachedCandidateBookIds,
                safeLongList(session.getShownBookIds())
        );

        AiRecommendationContext context = AiRecommendationContext.builder()
                .userId(userId)
                .sessionId(resolvedSessionId)
                .userInput(normalizedInput)
                .messageType(messageType)
                .conversationSummary(conversationSummary)
                .parsedIntent(parsedIntent)
                .candidateBooks(candidateResult.finalBooks)
                .build();

        AiRecommendationResponseVO response = buildAnswer(context);
        response.setSessionId(resolvedSessionId);
        response.setPhase(PHASE_RECOMMENDED);
        response.setIsNewBatch(newBatch);

        persistConversationAndState(userId, resolvedSessionId, normalizedInput, response, parsedIntent, candidateResult);
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
                .phase(PHASE_INIT)
                .answer("会话已重置，你可以重新告诉我想看什么样的书。")
                .followUpSuggestions(List.of("想看轻松一点的小说", "给我推荐能引发讨论的书", "我想看女性作者作品"))
                .build();
    }

    private AiRecommendationResponseVO handleInitRecommendation(Long userId,
                                                                 String resolvedSessionId,
                                                                 AiConversationSession session,
                                                                 String messageType) {
        AiParsedIntent initIntent = AiParsedIntent.builder()
                .intent("INIT_BASELINE")
                .querySummary("用户请求初始化画像推荐")
                .intentConfidence(0.7D)
                .needClarify(false)
                .build();

        CandidateResolveResult candidateResult = resolveBaselineCandidates(userId, session);
        if (candidateResult.finalBooks.isEmpty()) {
            return buildEmptyResultResponse(resolvedSessionId, false);
        }

        AiRecommendationContext context = AiRecommendationContext.builder()
                .userId(userId)
                .sessionId(resolvedSessionId)
                .userInput("获取 AI 推荐")
                .messageType(messageType)
                .conversationSummary(aiConversationService.summarizeConversation(session))
                .parsedIntent(initIntent)
                .candidateBooks(candidateResult.finalBooks)
                .build();

        AiRecommendationResponseVO response = buildAnswer(context);
        response.setSessionId(resolvedSessionId);
        response.setPhase(PHASE_INIT);
        response.setIsNewBatch(false);

        persistConversationAndState(userId, resolvedSessionId, "获取 AI 推荐", response, initIntent, candidateResult);
        return response;
    }

    private AiRecommendationResponseVO handleClarifyQuestion(Long userId,
                                                              String sessionId,
                                                              String userInput,
                                                              AiParsedIntent intent) {
        String question = intent.getClarifyQuestion();
        if (question == null || question.isBlank()) {
            question = defaultClarifyQuestion();
            intent.setClarifyQuestion(question);
        }
        intent.setNeedClarify(true);

        AiRecommendationResponseVO response = AiRecommendationResponseVO.builder()
                .type("AI_CLARIFY_QUESTION")
                .sessionId(sessionId)
                .phase(PHASE_NEED_CLARIFY)
                .answer(question)
                .followUpSuggestions(List.of("我想看心理学入门", "偏奇幻成长向", "只看女性作者"))
                .books(List.of())
                .isNewBatch(false)
                .build();

        aiConversationService.appendTurn(sessionId, userId, AiConversationTurn.builder()
                .role("user")
                .content(userInput)
                .createdAt(LocalDateTime.now())
                .build());

        aiConversationService.appendTurn(sessionId, userId, AiConversationTurn.builder()
                .role("assistant")
                .content(question)
                .createdAt(LocalDateTime.now())
                .metadata(Map.of("phase", PHASE_NEED_CLARIFY))
                .build());

        aiConversationService.updateContext(sessionId, userId, intent, List.of());
        aiConversationService.updateRecommendationState(
                sessionId,
                userId,
                PHASE_NEED_CLARIFY,
                buildIntentDigest(intent),
                question,
                List.of(),
                safeLongList(null)
        );
        return response;
    }

    private void persistConversationAndState(Long userId,
                                             String sessionId,
                                             String userInput,
                                             AiRecommendationResponseVO response,
                                             AiParsedIntent intent,
                                             CandidateResolveResult candidateResult) {
        aiConversationService.appendTurn(sessionId, userId, AiConversationTurn.builder()
                .role("user")
                .content(userInput)
                .createdAt(LocalDateTime.now())
                .build());

        aiConversationService.appendTurn(sessionId, userId, AiConversationTurn.builder()
                .role("assistant")
                .content(response.getAnswer())
                .createdAt(LocalDateTime.now())
                .metadata(Map.of(
                        "bookCount", response.getBooks().size(),
                        "phase", response.getPhase() == null ? PHASE_RECOMMENDED : response.getPhase()
                ))
                .build());

        List<AiRecommendedBook> books = response.getBooks().stream().map(this::toDomainBook).collect(Collectors.toList());
        aiConversationService.updateContext(sessionId, userId, intent, books);

        AiConversationSession latest = aiConversationService.loadSession(userId, sessionId);
        List<Long> shownBookIds = mergeShownBookIds(latest, response.getBooks());
        aiConversationService.updateRecommendationState(
                sessionId,
                userId,
                PHASE_RECOMMENDED,
                buildIntentDigest(intent),
                null,
                candidateResult.cachedCandidateBookIds,
                shownBookIds
        );
    }

    private CandidateResolveResult resolveBaselineCandidates(Long userId, AiConversationSession session) {
        Map<Long, PersonalizedRecVO> merged = new LinkedHashMap<>();
        mergeRecommendations(merged, recommendationService.getPersonalizedRecommendations(userId, RERANK_KEEP_LIMIT));
        mergeRecommendations(merged, recommendationService.getHotRecommendations(Math.max(HOT_ROUTE_TARGET, RERANK_KEEP_LIMIT / 3)));

        return finalizeCandidates(
                merged,
                AiParsedIntent.builder().intent("INIT_BASELINE").build(),
                "",
                session,
                false,
                false
        );
    }

    private AiParsedIntent parseIntent(String userInput, String conversationSummary, AiConversationSession session) {
        if (llmClient.isAvailable()) {
            try {
                LlmChatRequest request = aiPromptService.buildIntentRequest(userInput, conversationSummary);
                LlmChatResponse response = llmClient.chat(request);
                AiParsedIntent intent = parseIntentJson(response.getContent());
                if (intent != null) {
                    completeIntentDefaults(intent, userInput);
                    mergeSessionIntent(intent, session);
                    return intent;
                }
            } catch (Exception ignored) {
                // 真实模型不可用时降级到规则解析
            }
        }
        AiParsedIntent fallback = heuristicIntent(userInput);
        completeIntentDefaults(fallback, userInput);
        mergeSessionIntent(fallback, session);
        return fallback;
    }

    private void completeIntentDefaults(AiParsedIntent intent, String userInput) {
        if (intent.getQuerySummary() == null || intent.getQuerySummary().isBlank()) {
            intent.setQuerySummary(userInput);
        }

        enrichIntentByKeywords(intent, userInput);

        if (intent.getIntentConfidence() == null) {
            intent.setIntentConfidence(estimateIntentConfidence(intent, userInput));
        }

        if (intent.getNeedClarify() == null) {
            intent.setNeedClarify(false);
        }

        if (Boolean.TRUE.equals(intent.getNeedClarify())
                && (intent.getClarifyQuestion() == null || intent.getClarifyQuestion().isBlank())) {
            intent.setClarifyQuestion(defaultClarifyQuestion());
        }
    }

    private boolean needsClarify(AiParsedIntent intent, String userInput, AiConversationSession session) {
        if (intent == null) {
            return true;
        }

        // 有可召回信号时直接召回，不追问
        if (hasRecallSignals(intent, userInput)) {
            return false;
        }

        // 同一会话只允许追问一次，避免循环追问
        if (hasAskedClarify(session)) {
            return false;
        }

        if (Boolean.TRUE.equals(intent.getNeedClarify())) {
            return true;
        }

        String normalized = normalize(userInput);
        if (normalized.isBlank()) {
            return true;
        }
        if (normalized.length() <= 6) {
            return true;
        }
        if (normalized.contains("推荐") && normalized.contains("书")) {
            return true;
        }

        double confidence = intent.getIntentConfidence() == null ? 0D : intent.getIntentConfidence();
        if (confidence >= 0.45D) {
            return false;
        }
        return true;
    }

    private boolean hasRecallSignals(AiParsedIntent intent, String userInput) {
        if (intent == null) {
            return false;
        }
        boolean hasStructuredSignals = !(intent.getThemes().isEmpty()
                && intent.getPreferredAuthors().isEmpty()
                && intent.getPreferredCategories().isEmpty());
        if (hasStructuredSignals) {
            return true;
        }
        String normalized = normalize(userInput);
        return normalized.contains("奇幻")
                || normalized.contains("成长")
                || normalized.contains("治愈")
                || normalized.contains("温馨")
                || normalized.contains("心理")
                || normalized.contains("女性作者")
                || normalized.contains("推理")
                || normalized.contains("历史");
    }

    private boolean hasAskedClarify(AiConversationSession session) {
        if (session == null) {
            return false;
        }
        if (PHASE_NEED_CLARIFY.equals(session.getRecommendationPhase())) {
            return true;
        }
        if (session.getTurns() == null || session.getTurns().isEmpty()) {
            return false;
        }
        return session.getTurns().stream()
                .anyMatch(turn -> "assistant".equals(turn.getRole())
                        && turn.getMetadata() != null
                        && PHASE_NEED_CLARIFY.equals(String.valueOf(turn.getMetadata().get("phase"))));
    }

    private double estimateIntentConfidence(AiParsedIntent intent, String userInput) {
        double score = 0.3D;
        score += Math.min(0.4D, 0.1D * intent.getThemes().size());
        score += Math.min(0.2D, 0.1D * intent.getPreferredAuthors().size());
        score += Math.min(0.1D, 0.05D * intent.getPreferredCategories().size());
        if (intent.getConstraints() != null && !intent.getConstraints().isEmpty()) {
            score += 0.1D;
        }
        if (intent.getExclude() != null && !intent.getExclude().isEmpty()) {
            score += 0.1D;
        }

        String normalized = normalize(userInput);
        if (normalized.contains("推荐") && normalized.length() <= 6) {
            score -= 0.2D;
        }
        return Math.max(0D, Math.min(1D, score));
    }

    private void enrichIntentByKeywords(AiParsedIntent intent, String userInput) {
        if (intent == null || userInput == null) {
            return;
        }
        String normalized = normalize(userInput);
        if (normalized.contains("温馨") || normalized.contains("平和") || normalized.contains("平静")
                || normalized.contains("治愈") || normalized.contains("暖") || normalized.contains("轻松")) {
            addIfAbsent(intent.getThemes(), "治愈");
            addIfAbsent(intent.getThemes(), "温馨");
        }
        if (normalized.contains("慢节奏")) {
            addIfAbsent(intent.getConstraints(), "节奏平缓");
        }
        if (normalized.contains("不要太沉重") || normalized.contains("不压抑")) {
            addIfAbsent(intent.getExclude(), "沉重");
        }
    }

    private void addIfAbsent(List<String> list, String value) {
        if (list == null || value == null || value.isBlank()) {
            return;
        }
        if (!list.contains(value)) {
            list.add(value);
        }
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
                    .intentConfidence(root.has("intentConfidence") ? root.path("intentConfidence").asDouble(0D) : null)
                    .needClarify(root.has("needClarify") ? root.path("needClarify").asBoolean(false) : null)
                    .clarifyQuestion(root.path("clarifyQuestion").asText(null))
                    .build();
        } catch (Exception e) {
            return null;
        }
    }

    private CandidateResolveResult resolveCandidates(Long userId,
                                                     String userInput,
                                                     AiParsedIntent intent,
                                                     AiConversationSession session,
                                                     String messageType,
                                                     boolean newBatch) {
        boolean followUpSelection = isFollowUpSelection(messageType, userInput);
        boolean hasNewConstraints = hasNewConstraints(userInput, intent, session);

        Map<Long, PersonalizedRecVO> merged = new LinkedHashMap<>();

        if (followUpSelection && !newBatch && !hasNewConstraints && session != null
                && session.getLastBooks() != null && !session.getLastBooks().isEmpty()) {
            for (AiRecommendedBook lastBook : session.getLastBooks()) {
                merged.put(lastBook.getBookId(), PersonalizedRecVO.builder()
                        .bookId(lastBook.getBookId())
                        .bookTitle(lastBook.getBookTitle())
                        .author(lastBook.getAuthor())
                        .bookCover(lastBook.getBookCover())
                        .reason(lastBook.getReason())
                        .confidence(lastBook.getConfidence())
                        .score(lastBook.getScore())
                        .sourceStrategies(List.of("FOLLOW_UP_REUSE"))
                        .build());
            }
            return finalizeCandidates(merged, intent, userInput, session, newBatch, true);
        }

        // 优先命中会话候选，避免每轮都查库
        if (!newBatch && canReuseSessionCandidates(session, intent)) {
            mergeRecommendations(merged, loadSessionCandidateBooks(session.getCandidateBookIds()));
            mergeSearchResults(merged, searchBooks(userInput, intent, 8, 24));
            return finalizeCandidates(merged, intent, userInput, session, false, false);
        }

        boolean explicitIntent = hasExplicitIntent(userInput, intent);
        List<PersonalizedRecVO> intentCandidates = buildIntentDrivenCandidates(userInput, intent, INTENT_ROUTE_TARGET);
        mergeRecommendations(merged, intentCandidates);
        if (explicitIntent) {
            // 明确意图优先走意图召回；站内暂时没有精确结果时仍需提供基础推荐兜底。
            if (merged.isEmpty()) {
                mergeRecommendations(merged, recommendationService.getPersonalizedRecommendations(userId, PROFILE_ROUTE_TARGET));
                mergeRecommendations(merged, recommendationService.getHotRecommendations(HOT_ROUTE_TARGET));
            }
            return finalizeCandidates(merged, intent, userInput, session, newBatch, false);
        } else {
            mergeRecommendations(merged, recommendationService.getPersonalizedRecommendations(userId, PROFILE_ROUTE_TARGET));
            mergeRecommendations(merged, recommendationService.getHotRecommendations(HOT_ROUTE_TARGET));
        }

        return finalizeCandidates(merged, intent, userInput, session, newBatch, false);
    }

    private boolean hasExplicitIntent(String userInput, AiParsedIntent intent) {
        String normalized = normalize(userInput);
        if (normalized.isBlank() || intent == null) {
            return false;
        }
        boolean hasStructuredSignals = !(intent.getThemes().isEmpty()
                && intent.getPreferredAuthors().isEmpty()
                && intent.getPreferredCategories().isEmpty()
                && intent.getConstraints().isEmpty()
                && intent.getExclude().isEmpty());
        if (hasStructuredSignals) {
            return true;
        }
        return normalized.length() >= 4;
    }

    private boolean canReuseSessionCandidates(AiConversationSession session, AiParsedIntent intent) {
        if (session == null || session.getCandidateBookIds() == null || session.getCandidateBookIds().isEmpty()) {
            return false;
        }
        String sessionDigest = session.getIntentDigest();
        String currentDigest = buildIntentDigest(intent);
        if (sessionDigest == null || sessionDigest.isBlank()) {
            return false;
        }
        return Objects.equals(sessionDigest, currentDigest);
    }

    private List<PersonalizedRecVO> loadSessionCandidateBooks(List<Long> candidateBookIds) {
        if (candidateBookIds == null || candidateBookIds.isEmpty()) {
            return List.of();
        }
        List<Long> limitedIds = candidateBookIds.stream().limit(ROUGH_RECALL_LIMIT).collect(Collectors.toList());
        List<PersonalizedRecVO> records = bookUserMapper.getRecBookBaseByIds(limitedIds);
        if (records == null) {
            return List.of();
        }
        for (PersonalizedRecVO record : records) {
            record.setReason("复用当前会话候选并重新排序");
            record.setSourceStrategies(List.of("SESSION_CANDIDATE"));
            if (record.getConfidence() == null) {
                record.setConfidence(0.45D);
            }
        }
        return records;
    }

    private List<PersonalizedRecVO> buildIntentDrivenCandidates(String userInput, AiParsedIntent intent, int limit) {
        Map<Long, PersonalizedRecVO> merged = new LinkedHashMap<>();

        // 标签召回
        for (String tag : intent.getThemes()) {
            if (tag == null || tag.isBlank()) {
                continue;
            }
            List<RecommendationBookCandidate> records = userInterestTagMapper.getBooksByTagName(tag, Math.max(10, limit));
            if (records == null) {
                continue;
            }
            for (RecommendationBookCandidate record : records) {
                if (record.getBookId() == null) {
                    continue;
                }
                PersonalizedRecVO vo = merged.computeIfAbsent(record.getBookId(), key -> toRecVO(record));
                List<String> tags = vo.getMatchedTags() == null ? new ArrayList<>() : vo.getMatchedTags();
                if (!tags.contains(tag)) {
                    tags.add(tag);
                }
                vo.setMatchedTags(tags);
                vo.setSourceStrategies(appendSource(vo.getSourceStrategies(), "TAG_RECALL"));
                vo.setReason("命中你关注的标签方向");
            }
        }

        // 关键词/作者/分类召回（先复用站内搜索能力）
        List<PersonalizedRecVO> searchHits = searchBooks(userInput, intent, 12, Math.max(limit * 2, 40));
        mergeSearchResults(merged, searchHits);

        return merged.values().stream().limit(Math.max(limit, INTENT_ROUTE_TARGET)).collect(Collectors.toList());
    }

    private CandidateResolveResult finalizeCandidates(Map<Long, PersonalizedRecVO> merged,
                                                      AiParsedIntent intent,
                                                      String userInput,
                                                      AiConversationSession session,
                                                      boolean newBatch,
                                                      boolean allowReuseShown) {
        if (merged.isEmpty()) {
            return CandidateResolveResult.empty(newBatch);
        }

        Set<Long> excluded = new LinkedHashSet<>();
        if (!allowReuseShown && session != null && session.getShownBookIds() != null) {
            excluded.addAll(session.getShownBookIds());
        }
        if (newBatch && session != null && session.getLastBooks() != null) {
            excluded.addAll(session.getLastBooks().stream()
                    .map(AiRecommendedBook::getBookId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet()));
        }

        List<PersonalizedRecVO> scored = merged.values().stream()
                .filter(item -> item.getBookId() != null)
                .filter(item -> !excluded.contains(item.getBookId()))
                .peek(item -> item.setScore(scoreCandidate(item, intent, userInput)))
                .sorted(Comparator.comparing(PersonalizedRecVO::getScore, Comparator.nullsLast(Double::compareTo)).reversed())
                .collect(Collectors.toList());

        if (scored.isEmpty() && !allowReuseShown) {
            scored = merged.values().stream()
                    .filter(item -> item.getBookId() != null)
                    .peek(item -> item.setScore(scoreCandidate(item, intent, userInput)))
                    .sorted(Comparator.comparing(PersonalizedRecVO::getScore, Comparator.nullsLast(Double::compareTo)).reversed())
                    .collect(Collectors.toList());
        }

        List<PersonalizedRecVO> rerank = scored.stream().limit(RERANK_KEEP_LIMIT).collect(Collectors.toList());
        List<PersonalizedRecVO> output = rerank.stream().limit(RETURN_LIMIT).collect(Collectors.toList());

        return new CandidateResolveResult(
                output,
                rerank.stream().map(PersonalizedRecVO::getBookId).filter(Objects::nonNull).collect(Collectors.toList()),
                newBatch
        );
    }

    private List<String> appendSource(List<String> sourceStrategies, String source) {
        List<String> sources = sourceStrategies == null ? new ArrayList<>() : new ArrayList<>(sourceStrategies);
        if (!sources.contains(source)) {
            sources.add(source);
        }
        return sources;
    }

    private List<PersonalizedRecVO> searchBooks(String userInput, AiParsedIntent intent, int pageSize, int maxResults) {
        Set<String> keywords = new LinkedHashSet<>();
        if (userInput != null && !userInput.isBlank()) {
            keywords.add(userInput);
            keywords.addAll(splitIntentKeywords(userInput));
        }
        keywords.addAll(intent.getPreferredAuthors());
        keywords.addAll(intent.getThemes());
        keywords.addAll(intent.getPreferredCategories());

        List<PersonalizedRecVO> results = new ArrayList<>();
        for (String keyword : keywords) {
            if (keyword == null || keyword.isBlank() || keyword.length() < 2) {
                continue;
            }
            PageSearchDTO searchDTO = new PageSearchDTO();
            searchDTO.setKeyword(keyword);
            searchDTO.setPageNum(1);
            searchDTO.setPageSize(Math.max(3, pageSize));
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
                    if (results.size() >= maxResults) {
                        return results;
                    }
                }
            } catch (Exception ignored) {
                // 搜索失败时不阻断 AI 对话主链路
            }
        }
        return results;
    }

    private List<String> splitIntentKeywords(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        String[] tokens = text.split("[\\s,，。；;、/|+]+");
        List<String> results = new ArrayList<>();
        for (String token : tokens) {
            if (token == null) {
                continue;
            }
            String trimmed = token.trim();
            if (trimmed.length() >= 2 && trimmed.length() <= 12) {
                results.add(trimmed);
            }
        }
        return results;
    }

    private void mergeRecommendations(Map<Long, PersonalizedRecVO> merged, List<?> recommendations) {
        if (recommendations == null) {
            return;
        }
        for (Object item : recommendations) {
            PersonalizedRecVO recommendation = normalizeRecommendation(item);
            if (recommendation == null) {
                continue;
            }
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
                left.setSourceStrategies(appendSource(left.getSourceStrategies(), "SEARCH_MATCH"));
                return left;
            });
        }
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
        for (String categoryKeyword : intent.getPreferredCategories()) {
            String normalizedCategory = normalize(categoryKeyword);
            if (!normalizedCategory.isBlank() && reason.contains(normalizedCategory)) {
                score += 16D;
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

        if (answer == null || answer.isBlank() || answer.trim().length() < 40) {
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
        List<PersonalizedRecVO> topBooks = books.stream().limit(3).collect(Collectors.toList());
        String intro = "我根据你现在的需求，从站内书库里整理了一组更贴近“" + theme + "”方向的书。"
                + "这批书不是同一种风格的简单重复，而是尽量覆盖了相近主题下不同的阅读体验，方便你继续往下挑。";

        List<String> details = new ArrayList<>();
        for (PersonalizedRecVO book : topBooks) {
            String reason = summarizeBookReason(book);
            details.add("如果你更想读《" + book.getBookTitle() + "》这一路线，" + reason);
        }

        String closing = "你可以先从最符合当下兴趣的一本开始，如果想让我继续缩小范围，比如更治愈一点、节奏更快一点，或者只保留某一类题材，我可以继续帮你细化。";
        return intro + String.join("", details) + closing;
    }

    private String summarizeBookReason(PersonalizedRecVO book) {
        if (book.getReason() != null && !book.getReason().isBlank()) {
            return trimReason(book.getReason()) + "。";
        }
        if (book.getMatchedTags() != null && !book.getMatchedTags().isEmpty()) {
            return "它和你当前关注的“" + String.join("、", book.getMatchedTags()) + "”方向更接近";
        }
        if (book.getMatchedCategories() != null && !book.getMatchedCategories().isEmpty()) {
            return "它更贴近“" + String.join("、", book.getMatchedCategories()) + "”这一类阅读方向";
        }
        return "它的整体气质和你当前想找的书更接近";
    }

    private String trimReason(String reason) {
        String trimmed = reason.trim();
        if (trimmed.endsWith("。") || trimmed.endsWith("！") || trimmed.endsWith("？")) {
            return trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
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

    private boolean isInitRecommendationRequest(String messageType, String userInput) {
        if (userInput == null || userInput.isBlank()) {
            return true;
        }
        if (messageType == null) {
            return false;
        }
        String normalized = messageType.trim().toUpperCase(Locale.ROOT);
        return "INIT_RECOMMENDATION".equals(normalized)
                || "INIT".equals(normalized)
                || "START_SESSION".equals(normalized);
    }

    private boolean isFollowUpSelection(String messageType, String userInput) {
        if ("FOLLOW_UP".equalsIgnoreCase(messageType)) {
            return true;
        }
        String normalized = normalize(userInput);
        return normalized.contains("这几本") || normalized.contains("哪本") || normalized.contains("上一轮");
    }

    private boolean isNewBatchRequest(String messageType, String userInput) {
        if ("NEW_BATCH".equalsIgnoreCase(messageType)) {
            return true;
        }
        String normalized = normalize(userInput);
        return normalized.contains("还有别")
                || normalized.contains("换一批")
                || normalized.contains("别的推荐")
                || normalized.contains("更多推荐");
    }

    private boolean hasNewConstraints(String userInput, AiParsedIntent intent, AiConversationSession session) {
        String normalized = normalize(userInput);
        if (normalized.contains("不要") || normalized.contains("只要") || normalized.contains("更") || normalized.contains("太")) {
            return true;
        }
        if (session == null || session.getLastIntent() == null) {
            return true;
        }
        String currentDigest = buildIntentDigest(intent);
        String lastDigest = buildIntentDigest(session.getLastIntent());
        return !Objects.equals(currentDigest, lastDigest);
    }

    private String defaultClarifyQuestion() {
        return "你更偏向哪一类书？可以告诉我主题、作者偏好，或是否有“不要太长/不要太沉重”这类限制。";
    }

    private String buildIntentDigest(AiParsedIntent intent) {
        if (intent == null) {
            return "";
        }
        return String.join("|",
                intent.getIntent() == null ? "" : intent.getIntent(),
                String.join(",", intent.getThemes()),
                String.join(",", intent.getPreferredAuthors()),
                String.join(",", intent.getPreferredCategories()),
                String.join(",", intent.getConstraints()),
                String.join(",", intent.getExclude()),
                intent.getTone() == null ? "" : intent.getTone(),
                intent.getDifficulty() == null ? "" : intent.getDifficulty()
        );
    }

    private List<Long> mergeShownBookIds(AiConversationSession session, List<AiRecommendedBookVO> books) {
        Set<Long> merged = new LinkedHashSet<>(safeLongList(session == null ? null : session.getShownBookIds()));
        for (AiRecommendedBookVO book : books) {
            if (book.getBookId() != null) {
                merged.add(book.getBookId());
            }
        }
        return new ArrayList<>(merged);
    }

    private List<Long> safeLongList(List<Long> values) {
        if (values == null) {
            return new ArrayList<>();
        }
        return values.stream().filter(Objects::nonNull).collect(Collectors.toList());
    }

    private AiRecommendationResponseVO buildEmptyResultResponse(String sessionId, boolean newBatch) {
        return AiRecommendationResponseVO.builder()
                .type("AI_RECOMMENDATION_RESULT")
                .sessionId(sessionId)
                .phase(PHASE_RECOMMENDED)
                .isNewBatch(newBatch)
                .answer("当前可推荐的书不足，你可以补充更具体的标签、作者偏好或阅读限制，我会继续缩小范围。")
                .followUpSuggestions(List.of("心理学入门，篇幅短一些", "奇幻成长向，节奏快", "只要女性作者作品"))
                .books(List.of())
                .build();
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
        if (normalized.contains("温馨")) themes.add("温馨");
        if (normalized.contains("平和") || normalized.contains("平静")) themes.add("治愈");
        if (normalized.contains("哈利波特")) themes.add("魔法成长");

        if (normalized.contains("不要太长") || normalized.contains("短一点")) constraints.add("篇幅短");
        if (normalized.contains("女性作者")) constraints.add("女性作者");
        if (normalized.contains("入门")) constraints.add("适合入门");
        if (normalized.contains("不要太沉重")) excludes.add("沉重");

        if (normalized.toLowerCase(Locale.ROOT).contains("罗琳") || normalized.contains("J.K.")) {
            authors.add("J.K.罗琳");
        }

        double confidence = 0.35D;
        if (!themes.isEmpty()) confidence += 0.25D;
        if (!authors.isEmpty()) confidence += 0.2D;
        if (!constraints.isEmpty()) confidence += 0.15D;

        boolean needClarify = themes.isEmpty() && authors.isEmpty() && constraints.isEmpty() && excludes.isEmpty();

        return AiParsedIntent.builder()
                .intent("RECOMMEND_BOOKS")
                .themes(themes)
                .preferredAuthors(authors)
                .preferredCategories(new ArrayList<>())
                .constraints(constraints)
                .exclude(excludes)
                .querySummary(normalized)
                .intentConfidence(Math.min(1D, confidence))
                .needClarify(needClarify)
                .clarifyQuestion(needClarify ? defaultClarifyQuestion() : null)
                .build();
    }

    private List<String> readStringList(JsonNode root, String fieldName) {
        JsonNode node = root.path(fieldName);
        if (!node.isArray()) {
            return new ArrayList<>();
        }
        return objectMapper.convertValue(node, new TypeReference<List<String>>() {});
    }

    private PersonalizedRecVO normalizeRecommendation(Object item) {
        if (item == null) {
            return null;
        }
        if (item instanceof PersonalizedRecVO) {
            return (PersonalizedRecVO) item;
        }
        if (!(item instanceof Map)) {
            return objectMapper.convertValue(item, PersonalizedRecVO.class);
        }

        Map<?, ?> source = (Map<?, ?>) item;
        return PersonalizedRecVO.builder()
                .recommendationId(asLong(source.get("recommendationId")))
                .userId(asLong(source.get("userId")))
                .bookId(asLong(source.get("bookId")))
                .bookTitle(asString(source.get("bookTitle")))
                .author(asString(source.get("author")))
                .bookCover(asString(source.get("bookCover")))
                .score(asDouble(source.get("score")))
                .reason(asString(source.get("reason")))
                .reasonCodes(asStringList(source.get("reasonCodes")))
                .matchedTags(asStringList(source.get("matchedTags")))
                .matchedCategories(asStringList(source.get("matchedCategories")))
                .matchedAuthors(asStringList(source.get("matchedAuthors")))
                .sourceStrategies(asStringList(source.get("sourceStrategies")))
                .algorithmType(asString(source.get("algorithmType")))
                .recommendTime(asLocalDateTime(source.get("recommendTime")))
                .confidence(asDouble(source.get("confidence")))
                .relatedInterests(asString(source.get("relatedInterests")))
                .isRead(asBoolean(source.get("isRead")))
                .isCollected(asBoolean(source.get("isCollected")))
                .expireTime(asLocalDateTime(source.get("expireTime")))
                .build();
    }

    private PersonalizedRecVO toRecVO(RecommendationBookCandidate item) {
        return PersonalizedRecVO.builder()
                .bookId(item.getBookId())
                .bookTitle(item.getBookTitle())
                .author(item.getAuthor())
                .bookCover(item.getBookCover())
                .score(item.getHotScore() == null ? 65D : item.getHotScore())
                .confidence(0.6D)
                .matchedCategories(item.getCategoryName() == null ? List.of() : List.of(item.getCategoryName()))
                .sourceStrategies(List.of("TAG_RECALL"))
                .algorithmType("INTENT_RECALL")
                .reason("命中你当前提到的标签/主题")
                .build();
    }

    private Long asLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        String text = value.toString().trim();
        if (text.isEmpty()) {
            return null;
        }
        return Long.parseLong(text);
    }

    private Double asDouble(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        String text = value.toString().trim();
        if (text.isEmpty()) {
            return null;
        }
        return Double.parseDouble(text);
    }

    private Boolean asBoolean(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        String text = value.toString().trim();
        if (text.isEmpty()) {
            return null;
        }
        return Boolean.parseBoolean(text);
    }

    private String asString(Object value) {
        return value == null ? null : value.toString();
    }

    private List<String> asStringList(Object value) {
        if (value == null) {
            return new ArrayList<>();
        }
        if (value instanceof List) {
            List<?> source = (List<?>) value;
            return source.stream().map(String::valueOf).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    private LocalDateTime asLocalDateTime(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDateTime) {
            return (LocalDateTime) value;
        }
        String text = value.toString().trim();
        if (text.isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(text, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (DateTimeParseException ignored) {
            try {
                return LocalDateTime.parse(text.replace(" ", "T"));
            } catch (DateTimeParseException ignoredAgain) {
                return null;
            }
        }
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

    private static class CandidateResolveResult {

        private final List<PersonalizedRecVO> finalBooks;
        private final List<Long> cachedCandidateBookIds;
        private final boolean newBatch;

        private CandidateResolveResult(List<PersonalizedRecVO> finalBooks, List<Long> cachedCandidateBookIds, boolean newBatch) {
            this.finalBooks = finalBooks;
            this.cachedCandidateBookIds = cachedCandidateBookIds;
            this.newBatch = newBatch;
        }

        private static CandidateResolveResult empty(boolean newBatch) {
            return new CandidateResolveResult(List.of(), List.of(), newBatch);
        }
    }
}
