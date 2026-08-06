package com.cc.booktalk.like;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.cc.booktalk.application.user.service.like.impl.LikeRecordServiceImpl;
import com.cc.booktalk.application.user.service.recommendation.behavior.UserBehaviorEventDispatchService;
import com.cc.booktalk.common.context.UserContext;
import com.cc.booktalk.common.event.NotificationEventPublisher;
import com.cc.booktalk.domain.entity.like.LikeRecord;
import com.cc.booktalk.domain.entity.review.BookReview;
import com.cc.booktalk.infrastructure.persistence.user.mapper.book.BookUserMapper;
import com.cc.booktalk.infrastructure.persistence.user.mapper.comment.CommentUserMapper;
import com.cc.booktalk.infrastructure.persistence.user.mapper.like.LikeRecordMapper;
import com.cc.booktalk.infrastructure.persistence.user.mapper.post.PostMapper;
import com.cc.booktalk.infrastructure.persistence.user.mapper.recommendation.UserInfoUserMapper;
import com.cc.booktalk.infrastructure.persistence.user.mapper.review.ReviewUserMapper;
import com.cc.booktalk.interfaces.dto.user.UserDTO;
import com.cc.booktalk.interfaces.dto.user.like.LikeRecordDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LikeRecordServiceImplTest {

    @InjectMocks
    private LikeRecordServiceImpl likeRecordService;

    @Mock private LikeRecordMapper likeRecordMapper;
    @Mock private ReviewUserMapper reviewUserMapper;
    @Mock private PostMapper postMapper;
    @Mock private CommentUserMapper commentUserMapper;
    @Mock private BookUserMapper bookUserMapper;
    @Mock private UserInfoUserMapper userInfoUserMapper;
    @Mock private NotificationEventPublisher notificationEventPublisher;
    @Mock private UserBehaviorEventDispatchService userBehaviorEventDispatchService;
    @Mock private RedisTemplate<String, String> customStringRedisTemplate;
    @Mock private RedisTemplate<String, Object> customObjectRedisTemplate;
    @Mock private SetOperations<String, String> setOperations;
    @Mock private ValueOperations<String, Object> valueOperations;

    @AfterEach
    void clearUserContext() {
        UserContext.removeUser();
    }

    @Test
    void clickLikeWritesDatabaseBeforeSynchronizingCache() {
        UserContext.saveUser(UserDTO.builder().id(1L).nickname("tester").build());
        LikeRecordDTO dto = new LikeRecordDTO(null, 10L, "REVIEW", 999L);
        when(likeRecordMapper.selectByUserTaeget(1L, "REVIEW", 10L)).thenReturn(null);
        when(likeRecordMapper.insert(any(LikeRecord.class))).thenReturn(1);
        when(likeRecordMapper.selectCount(any(Wrapper.class))).thenReturn(1L);
        when(reviewUserMapper.selectById(10L)).thenReturn(BookReview.builder().id(10L).userId(1L).build());
        when(customStringRedisTemplate.opsForSet()).thenReturn(setOperations);
        when(customObjectRedisTemplate.opsForValue()).thenReturn(valueOperations);

        likeRecordService.clickLike(dto);

        InOrder databaseOrder = inOrder(likeRecordMapper, reviewUserMapper);
        databaseOrder.verify(likeRecordMapper).selectByUserTaeget(1L, "REVIEW", 10L);
        databaseOrder.verify(likeRecordMapper).insert(any(LikeRecord.class));
        databaseOrder.verify(reviewUserMapper).update(any(), any(Wrapper.class));
        databaseOrder.verify(likeRecordMapper).selectCount(any(Wrapper.class));
        verify(setOperations).add("like:user:1", "REVIEW:10");
        verify(setOperations).add("like:target:REVIEW:10", "1");
        verify(valueOperations).set("like:count:REVIEW:10", 1L);
    }
}
