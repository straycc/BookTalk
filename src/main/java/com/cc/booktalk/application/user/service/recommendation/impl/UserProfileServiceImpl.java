package com.cc.booktalk.application.user.service.recommendation.impl;

import com.cc.booktalk.application.user.service.recommendation.UserProfileService;
import com.cc.booktalk.application.user.service.recommendation.model.UserProfileSnapshot;
import com.cc.booktalk.domain.entity.recommendation.UserInterestTag;
import com.cc.booktalk.domain.enums.InterestType;
import com.cc.booktalk.infrastructure.persistence.user.mapper.recommendation.UserInterestTagMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
public class UserProfileServiceImpl implements UserProfileService {

    private static final double TIME_DECAY_FACTOR = 0.95;

    @Resource
    private UserInterestTagMapper userInterestTagMapper;

    @Override
    public UserProfileSnapshot buildSnapshot(Long userId) {
        if (userId == null) {
            return emptySnapshot(null);
        }

        List<UserInterestTag> allInterests = userInterestTagMapper.getTopUserInterests(userId, 20);
        if (allInterests == null || allInterests.isEmpty()) {
            return emptySnapshot(userId);
        }

        List<UserInterestTag> decayedInterests = allInterests.stream()
                .map(this::applyDecay)
                .sorted((left, right) -> Double.compare(
                        right.getInterestScore() == null ? 0D : right.getInterestScore(),
                        left.getInterestScore() == null ? 0D : left.getInterestScore()
                ))
                .collect(Collectors.toList());

        return UserProfileSnapshot.builder()
                .userId(userId)
                .interests(decayedInterests)
                .topTags(getTopKeys(decayedInterests, InterestType.TAG, 5))
                .topCategoryIds(getTopCategoryIds(decayedInterests, 3))
                .topAuthors(getTopKeys(decayedInterests, InterestType.AUTHOR, 3))
                .build();
    }

    private UserProfileSnapshot emptySnapshot(Long userId) {
        return UserProfileSnapshot.builder()
                .userId(userId)
                .interests(Collections.emptyList())
                .topTags(Collections.emptyList())
                .topCategoryIds(Collections.emptyList())
                .topAuthors(Collections.emptyList())
                .build();
    }

    private UserInterestTag applyDecay(UserInterestTag interest) {
        if (interest == null || interest.getUpdateTime() == null || interest.getInterestScore() == null) {
            return interest;
        }
        long days = Math.max(0, Duration.between(interest.getUpdateTime(), LocalDateTime.now()).toDays());
        if (days == 0) {
            return interest;
        }
        UserInterestTag copy = UserInterestTag.builder()
                .id(interest.getId())
                .userId(interest.getUserId())
                .interestType(interest.getInterestType())
                .interestKey(interest.getInterestKey())
                .interestScore(interest.getInterestScore() * Math.pow(TIME_DECAY_FACTOR, days))
                .behaviorCount(interest.getBehaviorCount())
                .updateTime(interest.getUpdateTime())
                .createTime(interest.getCreateTime())
                .build();
        return copy;
    }

    private List<String> getTopKeys(List<UserInterestTag> interests, InterestType type, int limit) {
        return interests.stream()
                .filter(Objects::nonNull)
                .filter(item -> type.name().equalsIgnoreCase(item.getInterestType()))
                .map(UserInterestTag::getInterestKey)
                .filter(Objects::nonNull)
                .limit(limit)
                .collect(Collectors.toList());
    }

    private List<Long> getTopCategoryIds(List<UserInterestTag> interests, int limit) {
        return interests.stream()
                .filter(Objects::nonNull)
                .filter(item -> InterestType.CATEGORY.name().equalsIgnoreCase(item.getInterestType()))
                .map(UserInterestTag::getInterestKey)
                .map(this::parseCategoryId)
                .filter(Objects::nonNull)
                .limit(limit)
                .collect(Collectors.toList());
    }

    private Long parseCategoryId(String value) {
        try {
            return value == null ? null : Long.parseLong(value);
        } catch (Exception e) {
            log.warn("解析分类兴趣失败: value={}", value);
            return null;
        }
    }
}
