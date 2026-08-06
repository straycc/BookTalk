package com.cc.booktalk.domain.post;

import com.cc.booktalk.domain.entity.recommendation.UserBehaviorLog;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class PostHotScoreDomainService {

    private static final Map<String, Double> WEIGHTS = Map.of(
            "POST_VIEW", 0.2,
            "POST_LIKE", 2.0,
            "POST_COMMENT", 4.0,
            "POST_REPLY", 3.0
    );

    private static final double DEFAULT_WEIGHT = 1.0;
    private static final double LAMBDA = Math.log(2.0) / 3.0;

    public double calculateHotScore(List<UserBehaviorLog> behaviors, LocalDateTime now) {
        if (behaviors == null || behaviors.isEmpty()) {
            return 0.0;
        }
        double rawScore = 0.0;
        for (UserBehaviorLog behavior : behaviors) {
            double baseWeight = WEIGHTS.getOrDefault(behavior.getBehaviorType(), DEFAULT_WEIGHT);
            double hoursDiff = Duration.between(behavior.getCreateTime(), now).toHours();
            double daysDiff = Math.max(0.0, hoursDiff / 24.0);
            double timeDecay = Math.exp(-LAMBDA * daysDiff);
            rawScore += baseWeight * timeDecay;
        }
        return Math.log10(rawScore + 1.0);
    }
}
