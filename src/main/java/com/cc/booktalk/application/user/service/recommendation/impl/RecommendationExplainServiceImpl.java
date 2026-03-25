package com.cc.booktalk.application.user.service.recommendation.impl;

import com.cc.booktalk.application.user.service.recommendation.RecommendationExplainService;
import com.cc.booktalk.application.user.service.recommendation.model.RecommendationBookCandidate;
import com.cc.booktalk.interfaces.vo.user.rec.PersonalizedRecVO;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class RecommendationExplainServiceImpl implements RecommendationExplainService {

    @Override
    public List<PersonalizedRecVO> buildRecommendations(List<RecommendationBookCandidate> candidates) {
        List<PersonalizedRecVO> results = new ArrayList<>();
        for (RecommendationBookCandidate candidate : candidates) {
            List<String> reasonCodes = new ArrayList<>(candidate.getReasonCodes());
            if (!candidate.getMatchedTags().isEmpty()) {
                reasonCodes.add("TAG_MATCH");
            }
            if (!candidate.getMatchedCategories().isEmpty()) {
                reasonCodes.add("CATEGORY_MATCH");
            }
            if (!candidate.getMatchedAuthors().isEmpty()) {
                reasonCodes.add("AUTHOR_MATCH");
            }
            if (reasonCodes.isEmpty()) {
                reasonCodes.add("HOT_FALLBACK");
            }

            PersonalizedRecVO recommendation = PersonalizedRecVO.builder()
                    .bookId(candidate.getBookId())
                    .bookTitle(candidate.getBookTitle())
                    .author(candidate.getAuthor())
                    .bookCover(candidate.getBookCover())
                    .score(candidate.getScore())
                    .reason(buildReason(candidate))
                    .reasonCodes(reasonCodes)
                    .matchedTags(new ArrayList<>(candidate.getMatchedTags()))
                    .matchedCategories(new ArrayList<>(candidate.getMatchedCategories()))
                    .matchedAuthors(new ArrayList<>(candidate.getMatchedAuthors()))
                    .sourceStrategies(new ArrayList<>(candidate.getSourceStrategies()))
                    .confidence(candidate.getConfidence())
                    .recommendTime(candidate.getRecommendTime() == null ? LocalDateTime.now() : candidate.getRecommendTime())
                    .algorithmType("HYBRID")
                    .relatedInterests(buildRelatedInterests(candidate))
                    .build();
            results.add(recommendation);
        }
        return results;
    }

    private String buildReason(RecommendationBookCandidate candidate) {
        if (!candidate.getMatchedTags().isEmpty() && !candidate.getMatchedCategories().isEmpty()) {
            return String.format("命中您关注的标签“%s”和分类“%s”",
                    candidate.getMatchedTags().iterator().next(),
                    candidate.getMatchedCategories().iterator().next());
        }
        if (!candidate.getMatchedTags().isEmpty()) {
            return String.format("命中您关注的标签“%s”", candidate.getMatchedTags().iterator().next());
        }
        if (!candidate.getMatchedCategories().isEmpty()) {
            return String.format("命中您关注的分类“%s”", candidate.getMatchedCategories().iterator().next());
        }
        if (!candidate.getMatchedAuthors().isEmpty()) {
            return String.format("命中您偏好的作者“%s”", candidate.getMatchedAuthors().iterator().next());
        }
        return "结合热门趋势与图书质量为您推荐";
    }

    private String buildRelatedInterests(RecommendationBookCandidate candidate) {
        List<String> parts = new ArrayList<>();
        parts.addAll(candidate.getMatchedTags());
        parts.addAll(candidate.getMatchedCategories());
        parts.addAll(candidate.getMatchedAuthors());
        return String.join(" / ", parts);
    }
}
