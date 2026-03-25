package com.cc.booktalk.ai;

import com.cc.booktalk.application.user.service.ai.impl.AiPromptServiceImpl;
import com.cc.booktalk.domain.ai.AiParsedIntent;
import com.cc.booktalk.domain.ai.AiRecommendationContext;
import com.cc.booktalk.infrastructure.ai.model.LlmChatRequest;
import com.cc.booktalk.interfaces.vo.user.rec.PersonalizedRecVO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class AiPromptServiceImplTest {

    private final AiPromptServiceImpl promptService = new AiPromptServiceImpl();

    @Test
    public void shouldBuildIntentPromptWithJsonRequirement() {
        LlmChatRequest request = promptService.buildIntentRequest("我想看更治愈一点的书", "上次推荐：百年孤独");

        Assertions.assertEquals(2, request.getMessages().size());
        Assertions.assertTrue(request.getMessages().get(0).getContent().contains("输出必须是 JSON"));
        Assertions.assertTrue(request.getMessages().get(1).getContent().contains("我想看更治愈一点的书"));
    }

    @Test
    public void shouldBuildAnswerPromptWithCandidateBooks() {
        AiRecommendationContext context = AiRecommendationContext.builder()
                .userId(1L)
                .sessionId("s1")
                .userInput("给我推荐成长向作品")
                .conversationSummary("上次需求：成长")
                .parsedIntent(AiParsedIntent.builder().themes(List.of("成长")).build())
                .candidateBooks(List.of(
                        PersonalizedRecVO.builder()
                                .bookId(1L)
                                .bookTitle("终身成长")
                                .author("卡罗尔")
                                .reason("命中成长标签")
                                .confidence(0.8)
                                .build()
                ))
                .build();

        LlmChatRequest request = promptService.buildAnswerRequest(context);

        Assertions.assertEquals(2, request.getMessages().size());
        Assertions.assertTrue(request.getMessages().get(1).getContent().contains("终身成长"));
        Assertions.assertTrue(request.getMessages().get(1).getContent().contains("bookId=1"));
    }
}
