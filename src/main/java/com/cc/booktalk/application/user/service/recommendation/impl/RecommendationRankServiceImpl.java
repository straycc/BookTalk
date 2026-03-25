package com.cc.booktalk.application.user.service.recommendation.impl;

import com.cc.booktalk.application.user.service.recommendation.RecommendationRankService;
import com.cc.booktalk.application.user.service.recommendation.model.RecommendationBookCandidate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RecommendationRankServiceImpl implements RecommendationRankService {

    @Override
    public List<RecommendationBookCandidate> rankCandidates(List<RecommendationBookCandidate> candidates, Integer limit) {
        int finalLimit = limit == null || limit <= 0 ? 10 : limit;
        return candidates.stream()
                .peek(this::scoreCandidate)
                .sorted(Comparator.comparing(RecommendationBookCandidate::getScore, Comparator.nullsLast(Double::compareTo)).reversed())
                .limit(finalLimit)
                .collect(Collectors.toList());
    }

    private void scoreCandidate(RecommendationBookCandidate candidate) {
        double tagScore = candidate.getMatchedTags().size() * 30D;
        double categoryScore = candidate.getMatchedCategories().size() * 20D;
        double authorScore = candidate.getMatchedAuthors().size() * 25D;
        double qualityScore = safe(candidate.getAverageScore()) * 4
                + logScore(candidate.getScoreCount(), 5)
                + logScore(candidate.getFavoriteCount(), 6);
        double hotScore = safe(candidate.getHotScore()) * 0.2;
        double freshnessScore = calculateFreshnessScore(candidate.getCreateTime());
        double totalScore = tagScore + categoryScore + authorScore + qualityScore + hotScore + freshnessScore;

        double confidence = Math.min(1D, 0.35
                + candidate.getSourceStrategies().size() * 0.15
                + candidate.getMatchedTags().size() * 0.1
                + candidate.getMatchedCategories().size() * 0.08
                + candidate.getMatchedAuthors().size() * 0.1);

        candidate.setScore(totalScore);
        candidate.setConfidence(confidence);
    }

    private double calculateFreshnessScore(LocalDateTime createTime) {
        if (createTime == null) {
            return 0D;
        }
        long days = Math.max(0, Duration.between(createTime, LocalDateTime.now()).toDays());
        if (days <= 30) {
            return 8D;
        }
        if (days <= 90) {
            return 4D;
        }
        return 0D;
    }

    private double logScore(Integer value, double multiplier) {
        int safeValue = value == null ? 0 : value;
        return Math.log10(safeValue + 1D) * multiplier;
    }

    private double safe(Double value) {
        return value == null ? 0D : value;
    }
}
