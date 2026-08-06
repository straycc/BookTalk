package com.cc.booktalk.review;

import com.cc.booktalk.application.user.service.content.ContentTagService;
import com.cc.booktalk.application.user.service.review.impl.ReviewUserServiceImpl;
import com.cc.booktalk.common.context.UserContext;
import com.cc.booktalk.domain.entity.book.Book;
import com.cc.booktalk.domain.entity.comment.Comment;
import com.cc.booktalk.domain.entity.like.LikeRecord;
import com.cc.booktalk.domain.entity.review.BookReview;
import com.cc.booktalk.infrastructure.persistence.user.mapper.book.BookUserMapper;
import com.cc.booktalk.infrastructure.persistence.user.mapper.comment.CommentUserMapper;
import com.cc.booktalk.infrastructure.persistence.user.mapper.like.LikeRecordMapper;
import com.cc.booktalk.infrastructure.persistence.user.mapper.notification.NotificationMapper;
import com.cc.booktalk.infrastructure.persistence.user.mapper.recommendation.UserInfoUserMapper;
import com.cc.booktalk.infrastructure.persistence.user.mapper.review.ReviewUserMapper;
import com.cc.booktalk.infrastructure.persistence.user.mapper.user.UserMapper;
import com.cc.booktalk.interfaces.dto.user.UserDTO;
import com.cc.booktalk.interfaces.dto.user.review.BookReviewDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewUserServiceImplTest {

    @InjectMocks private ReviewUserServiceImpl reviewService;
    @Mock private ReviewUserMapper reviewUserMapper;
    @Mock private BookUserMapper bookUserMapper;
    @Mock private CommentUserMapper commentUserMapper;
    @Mock private LikeRecordMapper likeRecordMapper;
    @Mock private NotificationMapper notificationMapper;
    @Mock private UserMapper userMapper;
    @Mock private UserInfoUserMapper userInfoUserMapper;
    @Mock private ContentTagService contentTagService;
    @Mock private RedisTemplate<String, String> customStringRedisTemplate;
    @Mock private RedisTemplate<String, Object> customObjectRedisTemplate;
    @Mock private SetOperations<String, String> setOperations;

    @AfterEach
    void clearUserContext() {
        UserContext.removeUser();
    }

    @Test
    void publishReviewRefreshesBookScoreStats() {
        UserContext.saveUser(UserDTO.builder().id(1L).build());
        when(bookUserMapper.selectById(20L)).thenReturn(Book.builder().id(20L).build());
        BookReviewDTO dto = reviewDto(20L, "A useful review", 9);

        reviewService.publish(dto);

        verify(reviewUserMapper).insert(any(BookReview.class));
        verify(bookUserMapper).refreshReviewScoreStats(20L);
    }

    @Test
    void updateReviewRefreshesBookScoreStats() {
        UserContext.saveUser(UserDTO.builder().id(1L).build());
        when(reviewUserMapper.selectById(10L)).thenReturn(
                BookReview.builder().id(10L).bookId(20L).userId(1L).build());

        reviewService.updateBookReview(10L, reviewDto(20L, "Updated review", 8));

        verify(reviewUserMapper).updateById(any(BookReview.class));
        verify(bookUserMapper).refreshReviewScoreStats(20L);
    }

    @Test
    void deleteReviewRemovesCommentLikesAndNotifications() {
        UserContext.saveUser(UserDTO.builder().id(1L).build());
        when(reviewUserMapper.selectById(10L)).thenReturn(
                BookReview.builder().id(10L).bookId(20L).userId(1L).build());
        when(commentUserMapper.selectList(any())).thenReturn(List.of(
                Comment.builder().id(100L).rootId(10L).userId(2L).build()));
        when(likeRecordMapper.selectList(any())).thenReturn(
                List.of(LikeRecord.builder().userId(3L).targetType("REVIEW").targetId(10L).build()),
                List.of(LikeRecord.builder().userId(4L).targetType("COMMENT").targetId(100L).build()));
        when(customStringRedisTemplate.opsForSet()).thenReturn(setOperations);

        reviewService.deleteBookReview(10L);

        verify(likeRecordMapper, times(2)).delete(any());
        verify(commentUserMapper).delete(any());
        verify(notificationMapper, times(2)).delete(any());
        verify(contentTagService).deleteContentTags(10L, "REVIEW");
        verify(reviewUserMapper).deleteById(10L);
        verify(bookUserMapper).refreshReviewScoreStats(20L);
        verify(setOperations).remove("like:user:3", "REVIEW:10");
        verify(setOperations).remove("like:user:4", "COMMENT:100");
    }

    private BookReviewDTO reviewDto(Long bookId, String content, Integer score) {
        BookReviewDTO dto = new BookReviewDTO();
        dto.setBookId(bookId);
        dto.setContent(content);
        dto.setScore(score);
        return dto;
    }
}
