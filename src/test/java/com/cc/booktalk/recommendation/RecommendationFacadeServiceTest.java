package com.cc.booktalk.recommendation;

import com.cc.booktalk.application.user.service.recommendation.HotRecommendationService;
import com.cc.booktalk.application.user.service.recommendation.impl.RecommendationFacadeService;
import com.cc.booktalk.application.user.service.recommendation.profile.UserProfileService;
import com.cc.booktalk.domain.recommendation.model.UserProfileSnapshot;
import com.cc.booktalk.domain.recommendation.model.RecommendationBookCandidate;
import com.cc.booktalk.domain.entity.bookShelf.BookShelf;
import com.cc.booktalk.domain.entity.review.BookReview;
import com.cc.booktalk.infrastructure.persistence.user.mapper.book.BookUserMapper;
import com.cc.booktalk.infrastructure.persistence.user.mapper.bookShelf.BookShelfMapper;
import com.cc.booktalk.infrastructure.persistence.user.mapper.recommendation.UserBehaviorLogMapper;
import com.cc.booktalk.infrastructure.persistence.user.mapper.recommendation.UserInfoUserMapper;
import com.cc.booktalk.infrastructure.persistence.user.mapper.recommendation.UserInterestTagMapper;
import com.cc.booktalk.infrastructure.persistence.user.mapper.review.ReviewUserMapper;
import com.cc.booktalk.interfaces.vo.user.rec.PersonalizedRecVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Collections;
import java.util.List;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommendationFacadeServiceTest {

    @InjectMocks private RecommendationFacadeService recommendationService;
    @Mock private UserProfileService userProfileService;
    @Mock private HotRecommendationService hotRecommendationService;
    @Mock private ReviewUserMapper reviewUserMapper;
    @Mock private UserInfoUserMapper userInfoUserMapper;
    @Mock private BookUserMapper bookUserMapper;
    @Mock private BookShelfMapper bookShelfMapper;
    @Mock private UserBehaviorLogMapper userBehaviorLogMapper;
    @Mock private UserInterestTagMapper userInterestTagMapper;
    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private ValueOperations<String, Object> valueOperations;

    @Test
    void newUserFallsBackToHotRecommendations() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(userProfileService.buildSnapshot(1L)).thenReturn(UserProfileSnapshot.builder()
                .userId(1L)
                .interests(Collections.emptyList())
                .topTags(Collections.emptyList())
                .topCategoryIds(Collections.emptyList())
                .topAuthors(Collections.emptyList())
                .build());
        when(hotRecommendationService.getHotRecommendations(30)).thenReturn(List.of(hotBook(10L)));

        List<PersonalizedRecVO> results = recommendationService.getPersonalizedRecommendations(1L, 10);

        assertEquals(1, results.size());
        assertEquals(10L, results.get(0).getBookId());
        assertEquals("POPULAR", results.get(0).getAlgorithmType());
    }

    @Test
    void redisFailureStillFallsBackToHotRecommendations() {
        when(redisTemplate.opsForValue()).thenThrow(new IllegalStateException("redis unavailable"));
        when(userProfileService.buildSnapshot(1L)).thenThrow(new IllegalStateException("profile unavailable"));
        when(hotRecommendationService.getHotRecommendations(30)).thenReturn(List.of(hotBook(11L)));

        List<PersonalizedRecVO> results = recommendationService.getPersonalizedRecommendations(1L, 10);

        assertEquals(1, results.size());
        assertEquals(11L, results.get(0).getBookId());
    }

    @Test
    void userWithInterestsReceivesMatchingPersonalizedRecommendation() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(userProfileService.buildSnapshot(2L)).thenReturn(UserProfileSnapshot.builder()
                .userId(2L)
                .topTags(List.of("software"))
                .topCategoryIds(Collections.emptyList())
                .topAuthors(Collections.emptyList())
                .interests(Collections.emptyList())
                .build());
        when(userInterestTagMapper.getBooksByTagName(eq("software"), anyInt()))
                .thenReturn(List.of(RecommendationBookCandidate.builder()
                        .bookId(20L)
                        .bookTitle("Clean Code")
                        .author("Robert C. Martin")
                        .averageScore(8.8)
                        .scoreCount(10)
                        .favoriteCount(5)
                        .hotScore(20.0)
                        .build()));
        when(hotRecommendationService.getHotRecommendations(anyInt())).thenReturn(Collections.emptyList());
        when(bookShelfMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(reviewUserMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(userBehaviorLogMapper.getStrongInteractedBookIds(2L, 180)).thenReturn(Collections.emptyList());

        List<PersonalizedRecVO> results = recommendationService.getPersonalizedRecommendations(2L, 10);

        assertEquals(1, results.size());
        assertEquals(20L, results.get(0).getBookId());
        assertEquals("HYBRID", results.get(0).getAlgorithmType());
        assertEquals(List.of("TAG_MATCH"), results.get(0).getReasonCodes());
    }

    @Test
    void cachedBooksAlreadyInShelfAreRecomputedInsteadOfReturned() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(List.of(hotBook(30L)));
        BookShelf shelfItem = new BookShelf();
        shelfItem.setBookId(30L);
        when(bookShelfMapper.selectList(any())).thenReturn(List.of(shelfItem));
        when(reviewUserMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(userBehaviorLogMapper.getStrongInteractedBookIds(1L, 180)).thenReturn(Collections.emptyList());
        when(userProfileService.buildSnapshot(1L)).thenReturn(UserProfileSnapshot.builder()
                .userId(1L)
                .interests(Collections.emptyList())
                .topTags(Collections.emptyList())
                .topCategoryIds(Collections.emptyList())
                .topAuthors(Collections.emptyList())
                .build());
        when(hotRecommendationService.getHotRecommendations(30)).thenReturn(List.of(hotBook(31L)));

        List<PersonalizedRecVO> results = recommendationService.getPersonalizedRecommendations(1L, 10);

        assertEquals(List.of(31L), results.stream().map(PersonalizedRecVO::getBookId).collect(java.util.stream.Collectors.toList()));
    }

    @Test
    void hotReviewRedisFailureFallsBackToLatestReviews() {
        when(redisTemplate.opsForValue()).thenThrow(new IllegalStateException("redis unavailable"));
        when(userBehaviorLogMapper.getHotReviewCandidateIds(7, 100)).thenReturn(Collections.emptyList());
        when(reviewUserMapper.selectList(any())).thenReturn(List.of(BookReview.builder()
                .id(40L)
                .bookId(20L)
                .userId(2L)
                .content("Useful review")
                .createTime(LocalDateTime.now())
                .build()));

        assertEquals(1, recommendationService.getHotReviewRecommendations("7d", 1).size());
    }

    private PersonalizedRecVO hotBook(Long bookId) {
        return PersonalizedRecVO.builder()
                .bookId(bookId)
                .bookTitle("book")
                .score(1D)
                .build();
    }
}
