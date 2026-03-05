package com.cc.booktalk.domain.recommendation;

import com.cc.booktalk.domain.entity.recommendation.UserBehaviorLog;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class HotReviewRecDomain {

    private static final Map<String, Double> WEIGHTS = Map.of(
            "REVIEW_LIKE", 2.0,
            "REVIEW_COMMENT", 2.5,
            "REVIEW_REPLY", 3.0
    );

    private static final double DEFAULT_WEIGHT = 1.0;

    // 3天半衰期：lambda = ln(2)/3
    private static final double LAMBDA = Math.log(2.0) / 3.0;

    // 最低行为门槛
    private static final int MIN_ACTIONS = 2;

    public boolean enoughActions(int actionCount) {
        return actionCount >= MIN_ACTIONS;
    }

    public double calculateHotScore(List<UserBehaviorLog> behaviors, LocalDateTime now) {
        if (behaviors == null || behaviors.isEmpty()) {
            return 0.0;
        }

        double totalScore = 0.0;
        for (UserBehaviorLog b : behaviors) {
            double baseWeight = WEIGHTS.getOrDefault(
                    b.getBehaviorType() == null ? "" : b.getBehaviorType().trim().toUpperCase(),
                    DEFAULT_WEIGHT
            );

            double hoursDiff = Duration.between(b.getCreateTime(), now).toHours();
            double daysDiff = Math.max(0.0, hoursDiff / 24.0);
            double decay = Math.exp(-LAMBDA * daysDiff);
            totalScore += baseWeight * decay;
        }

        return Math.log10(totalScore + 1);
    }
}

