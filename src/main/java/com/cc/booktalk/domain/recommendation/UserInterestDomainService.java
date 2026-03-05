package com.cc.booktalk.domain.recommendation;

import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class UserInterestDomainService {

    public boolean isValidBehavior(Long userId, Long targetId, Double score, String behaviorType) {
        return userId != null && targetId != null && score != null && behaviorType != null && !
                behaviorType.isBlank();
    }

    public Long resolveBookIdByBehavior(String behaviorType, Long targetId, Long reviewBookId) {
        if (behaviorType == null) {
            return null;
        }
        String normalizedBehaviorType = behaviorType.trim().toUpperCase();
        if (normalizedBehaviorType.startsWith("BOOK_")) return targetId;
        if (normalizedBehaviorType.startsWith("REVIEW_")) return reviewBookId;
        return null;
    }

    public List<String> resolveInterestTags(List<String> rawTags) {
        return rawTags == null ? List.of() : rawTags.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .collect(Collectors.toList());
    }
}
