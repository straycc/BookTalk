package com.cc.booktalk.application.user.service.recommendation.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cc.booktalk.application.user.service.recommendation.RecommendationFilterService;
import com.cc.booktalk.application.user.service.recommendation.model.RecommendationBookCandidate;
import com.cc.booktalk.domain.entity.bookShelf.BookShelf;
import com.cc.booktalk.domain.entity.review.BookReview;
import com.cc.booktalk.infrastructure.persistence.user.mapper.bookShelf.BookShelfMapper;
import com.cc.booktalk.infrastructure.persistence.user.mapper.recommendation.UserBehaviorLogMapper;
import com.cc.booktalk.infrastructure.persistence.user.mapper.review.ReviewUserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RecommendationFilterServiceImpl implements RecommendationFilterService {

    @Resource
    private BookShelfMapper bookShelfMapper;

    @Resource
    private ReviewUserMapper reviewUserMapper;

    @Resource
    private UserBehaviorLogMapper userBehaviorLogMapper;

    @Override
    public List<RecommendationBookCandidate> filterCandidates(Long userId, List<RecommendationBookCandidate> candidates) {
        if (userId == null || candidates == null || candidates.isEmpty()) {
            return candidates == null ? List.of() : candidates;
        }

        Set<Long> excludedBookIds = loadExcludedBookIds(userId);
        List<RecommendationBookCandidate> filtered = new ArrayList<>();
        for (RecommendationBookCandidate candidate : candidates) {
            if (candidate.getBookId() == null || excludedBookIds.contains(candidate.getBookId())) {
                continue;
            }
            filtered.add(candidate);
        }
        return filtered;
    }

    private Set<Long> loadExcludedBookIds(Long userId) {
        Set<Long> excluded = new HashSet<>();

        List<BookShelf> shelfItems = bookShelfMapper.selectList(new LambdaQueryWrapper<BookShelf>()
                .eq(BookShelf::getUserId, userId));
        excluded.addAll(shelfItems.stream().map(BookShelf::getBookId).collect(Collectors.toSet()));

        List<BookReview> reviews = reviewUserMapper.selectList(new LambdaQueryWrapper<BookReview>()
                .eq(BookReview::getUserId, userId));
        excluded.addAll(reviews.stream().map(BookReview::getBookId).collect(Collectors.toSet()));

        List<Long> strongInteracted = userBehaviorLogMapper.getStrongInteractedBookIds(userId, 180);
        excluded.addAll(strongInteracted);

        return excluded;
    }
}
