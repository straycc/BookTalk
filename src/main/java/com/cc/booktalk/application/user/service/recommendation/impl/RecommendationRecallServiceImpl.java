package com.cc.booktalk.application.user.service.recommendation.impl;

import com.cc.booktalk.application.user.service.recommendation.HotRecommendationService;
import com.cc.booktalk.application.user.service.recommendation.RecommendationRecallService;
import com.cc.booktalk.application.user.service.recommendation.model.RecommendationBookCandidate;
import com.cc.booktalk.application.user.service.recommendation.model.UserProfileSnapshot;
import com.cc.booktalk.infrastructure.persistence.user.mapper.book.BookUserMapper;
import com.cc.booktalk.infrastructure.persistence.user.mapper.recommendation.UserInterestTagMapper;
import com.cc.booktalk.interfaces.vo.user.rec.PersonalizedRecVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class RecommendationRecallServiceImpl implements RecommendationRecallService {

    @Resource
    private UserInterestTagMapper userInterestTagMapper;

    @Resource
    private BookUserMapper bookUserMapper;

    @Resource
    private HotRecommendationService hotRecommendationService;

    @Override
    public List<RecommendationBookCandidate> recallCandidates(UserProfileSnapshot snapshot, Integer limit) {
        int finalLimit = limit == null || limit <= 0 ? 10 : limit;
        int perRouteLimit = Math.max(finalLimit * 2, 10);
        Map<Long, RecommendationBookCandidate> merged = new LinkedHashMap<>();

        for (String tag : snapshot.getTopTags()) {
            mergeByTag(merged, tag, perRouteLimit);
        }
        if (!snapshot.getTopCategoryIds().isEmpty()) {
            mergeByCategories(merged, snapshot.getTopCategoryIds(), perRouteLimit);
        }
        if (!snapshot.getTopAuthors().isEmpty()) {
            mergeByAuthors(merged, snapshot.getTopAuthors(), perRouteLimit);
        }
        mergeHotFallback(merged, perRouteLimit);

        return new ArrayList<>(merged.values());
    }

    private void mergeByTag(Map<Long, RecommendationBookCandidate> merged, String tag, int limit) {
        List<RecommendationBookCandidate> books = userInterestTagMapper.getBooksByTagName(tag, limit);
        for (RecommendationBookCandidate candidate : books) {
            RecommendationBookCandidate target = merged.computeIfAbsent(candidate.getBookId(), key -> candidate);
            target.getMatchedTags().add(tag);
            target.getSourceStrategies().add("TAG_RECALL");
        }
    }

    private void mergeByCategories(Map<Long, RecommendationBookCandidate> merged, List<Long> categoryIds, int limit) {
        List<RecommendationBookCandidate> books = bookUserMapper.findBooksByCategoryIds(categoryIds, limit);
        for (RecommendationBookCandidate candidate : books) {
            RecommendationBookCandidate target = merged.computeIfAbsent(candidate.getBookId(), key -> candidate);
            if (candidate.getCategoryName() != null) {
                target.getMatchedCategories().add(candidate.getCategoryName());
            }
            target.getSourceStrategies().add("CATEGORY_RECALL");
        }
    }

    private void mergeByAuthors(Map<Long, RecommendationBookCandidate> merged, List<String> authors, int limit) {
        List<RecommendationBookCandidate> books = bookUserMapper.findBooksByAuthors(authors, limit);
        for (RecommendationBookCandidate candidate : books) {
            RecommendationBookCandidate target = merged.computeIfAbsent(candidate.getBookId(), key -> candidate);
            if (candidate.getAuthor() != null) {
                target.getMatchedAuthors().add(candidate.getAuthor());
            }
            target.getSourceStrategies().add("AUTHOR_RECALL");
        }
    }

    private void mergeHotFallback(Map<Long, RecommendationBookCandidate> merged, int limit) {
        List<PersonalizedRecVO> hotBooks = hotRecommendationService.getHotRecommendations(limit);
        for (PersonalizedRecVO hotBook : hotBooks) {
            RecommendationBookCandidate target = merged.computeIfAbsent(hotBook.getBookId(), key -> RecommendationBookCandidate.builder()
                    .bookId(hotBook.getBookId())
                    .bookTitle(hotBook.getBookTitle())
                    .author(hotBook.getAuthor())
                    .bookCover(hotBook.getBookCover())
                    .hotScore(hotBook.getScore())
                    .build());
            target.getSourceStrategies().add("HOT_FALLBACK");
        }
    }
}
