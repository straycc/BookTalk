package com.cc.booktalk.application.user.service.recommendation.model;

import com.cc.booktalk.domain.entity.recommendation.UserInterestTag;
import lombok.Builder;
import lombok.Data;

import java.util.Collections;
import java.util.List;

/**
 * 用户推荐画像快照
 */
@Data
@Builder
public class UserProfileSnapshot {

    private Long userId;
    private List<UserInterestTag> interests;
    private List<String> topTags;
    private List<Long> topCategoryIds;
    private List<String> topAuthors;

    public boolean isEmpty() {
        return (interests == null || interests.isEmpty())
                && (topTags == null || topTags.isEmpty())
                && (topCategoryIds == null || topCategoryIds.isEmpty())
                && (topAuthors == null || topAuthors.isEmpty());
    }

    public List<UserInterestTag> safeInterests() {
        return interests == null ? Collections.emptyList() : interests;
    }
}
