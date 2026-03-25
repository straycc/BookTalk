package com.cc.booktalk.recommendation;

import com.cc.booktalk.application.user.service.recommendation.impl.RecommendationRankServiceImpl;
import com.cc.booktalk.application.user.service.recommendation.model.RecommendationBookCandidate;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

public class RecommendationRankServiceImplTest {

    private final RecommendationRankServiceImpl rankService = new RecommendationRankServiceImpl();

    @Test
    public void shouldRankCandidateWithMoreMatchesHigher() {
        RecommendationBookCandidate strong = RecommendationBookCandidate.builder()
                .bookId(1L)
                .bookTitle("强匹配图书")
                .averageScore(9.0)
                .scoreCount(1000)
                .favoriteCount(600)
                .hotScore(120.0)
                .createTime(LocalDateTime.now().minusDays(5))
                .build();
        strong.getMatchedTags().add("历史");
        strong.getMatchedCategories().add("文学");
        strong.getMatchedAuthors().add("作者A");
        strong.getSourceStrategies().add("TAG_RECALL");
        strong.getSourceStrategies().add("AUTHOR_RECALL");

        RecommendationBookCandidate weak = RecommendationBookCandidate.builder()
                .bookId(2L)
                .bookTitle("弱匹配图书")
                .averageScore(7.5)
                .scoreCount(100)
                .favoriteCount(50)
                .hotScore(20.0)
                .createTime(LocalDateTime.now().minusDays(180))
                .build();
        weak.getSourceStrategies().add("HOT_FALLBACK");

        List<RecommendationBookCandidate> ranked = rankService.rankCandidates(List.of(weak, strong), 2);

        Assertions.assertEquals(2, ranked.size());
        Assertions.assertEquals(1L, ranked.get(0).getBookId());
        Assertions.assertTrue(ranked.get(0).getScore() > ranked.get(1).getScore());
        Assertions.assertTrue(ranked.get(0).getConfidence() >= ranked.get(1).getConfidence());
    }
}
