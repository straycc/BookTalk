package com.cc.booktalk.recommendation;

import com.cc.booktalk.application.user.service.recommendation.impl.RecommendationExplainServiceImpl;
import com.cc.booktalk.application.user.service.recommendation.model.RecommendationBookCandidate;
import com.cc.booktalk.interfaces.vo.user.rec.PersonalizedRecVO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class RecommendationExplainServiceImplTest {

    private final RecommendationExplainServiceImpl explainService = new RecommendationExplainServiceImpl();

    @Test
    public void shouldBuildStructuredRecommendationFields() {
        RecommendationBookCandidate candidate = RecommendationBookCandidate.builder()
                .bookId(1L)
                .bookTitle("测试图书")
                .author("测试作者")
                .bookCover("cover")
                .score(88.5)
                .confidence(0.91)
                .build();
        candidate.getMatchedTags().add("历史");
        candidate.getMatchedCategories().add("文学");
        candidate.getSourceStrategies().add("TAG_RECALL");
        candidate.getSourceStrategies().add("CATEGORY_RECALL");

        List<PersonalizedRecVO> results = explainService.buildRecommendations(List.of(candidate));

        Assertions.assertEquals(1, results.size());
        PersonalizedRecVO recommendation = results.get(0);
        Assertions.assertEquals("测试图书", recommendation.getBookTitle());
        Assertions.assertTrue(recommendation.getReason().contains("历史"));
        Assertions.assertTrue(recommendation.getReasonCodes().contains("TAG_MATCH"));
        Assertions.assertTrue(recommendation.getReasonCodes().contains("CATEGORY_MATCH"));
        Assertions.assertEquals(2, recommendation.getSourceStrategies().size());
        Assertions.assertFalse(recommendation.getMatchedTags().isEmpty());
        Assertions.assertFalse(recommendation.getMatchedCategories().isEmpty());
    }
}
