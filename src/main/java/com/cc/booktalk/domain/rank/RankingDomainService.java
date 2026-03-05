package com.cc.booktalk.domain.rank;

import com.cc.booktalk.domain.entity.recommendation.UserBehaviorLog;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class RankingDomainService {

    private static final Map<String, Double> BOOK_BEHAVIOR_WEIGHTS = Map.of(
            "BOOK_COLLECT", 4.0,
            "BOOK_REVIEW", 5.0,
            "BOOK_VIEW", 0.5
    );

    private static final Map<String, Double> REVIEW_BEHAVIOR_WEIGHTS = Map.of(
            "REVIEW_LIKE", 2.0,
            "REVIEW_REPLY", 3.0
    );

    private static final Double DEFAULT_BOOK_BEHAVIOR_WEIGHT = 1.0;
    private static final Double DEFAULT_REVIEW_BEHAVIOR_WEIGHT = 0.5;

    public double calculateBookHotScore(List<UserBehaviorLog> bookBehaviors, List<UserBehaviorLog> reviewBehaviors) {
        return sumBookBehaviorScore(bookBehaviors) + sumReviewBehaviorScore(reviewBehaviors);
    }

    public double calculateReviewHotScore(List<UserBehaviorLog> reviewBehaviors) {
        return sumReviewBehaviorScore(reviewBehaviors);
    }

    private double sumBookBehaviorScore(List<UserBehaviorLog> behaviors) {
        if (behaviors == null || behaviors.isEmpty()) {
            return 0.0;
        }
        double score = 0.0;
        for (UserBehaviorLog behavior : behaviors) {
            score += BOOK_BEHAVIOR_WEIGHTS.getOrDefault(
                    behavior.getBehaviorType(), DEFAULT_BOOK_BEHAVIOR_WEIGHT
            );
        }
        return score;
    }

    private double sumReviewBehaviorScore(List<UserBehaviorLog> behaviors) {
        if (behaviors == null || behaviors.isEmpty()) {
            return 0.0;
        }
        double score = 0.0;
        for (UserBehaviorLog behavior : behaviors) {
            score += REVIEW_BEHAVIOR_WEIGHTS.getOrDefault(
                    behavior.getBehaviorType(), DEFAULT_REVIEW_BEHAVIOR_WEIGHT
            );
        }
        return score;
    }
}
