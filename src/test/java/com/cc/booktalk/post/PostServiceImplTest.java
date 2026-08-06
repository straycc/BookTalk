package com.cc.booktalk.post;

import com.cc.booktalk.application.user.service.content.ContentTagService;
import com.cc.booktalk.application.user.service.post.impl.PostServiceImpl;
import com.cc.booktalk.common.context.UserContext;
import com.cc.booktalk.domain.entity.comment.Comment;
import com.cc.booktalk.domain.entity.like.LikeRecord;
import com.cc.booktalk.domain.entity.post.Post;
import com.cc.booktalk.infrastructure.persistence.user.mapper.book.BookUserMapper;
import com.cc.booktalk.infrastructure.persistence.user.mapper.comment.CommentUserMapper;
import com.cc.booktalk.infrastructure.persistence.user.mapper.like.LikeRecordMapper;
import com.cc.booktalk.infrastructure.persistence.user.mapper.notification.NotificationMapper;
import com.cc.booktalk.infrastructure.persistence.user.mapper.post.PostMapper;
import com.cc.booktalk.infrastructure.persistence.user.mapper.recommendation.UserInfoUserMapper;
import com.cc.booktalk.interfaces.dto.user.UserDTO;
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
class PostServiceImplTest {

    @InjectMocks private PostServiceImpl postService;
    @Mock private PostMapper postMapper;
    @Mock private CommentUserMapper commentUserMapper;
    @Mock private LikeRecordMapper likeRecordMapper;
    @Mock private NotificationMapper notificationMapper;
    @Mock private ContentTagService contentTagService;
    @Mock private UserInfoUserMapper userInfoUserMapper;
    @Mock private BookUserMapper bookUserMapper;
    @Mock private RedisTemplate<String, String> customStringRedisTemplate;
    @Mock private RedisTemplate<String, Object> customObjectRedisTemplate;
    @Mock private SetOperations<String, String> setOperations;

    @AfterEach
    void clearUserContext() {
        UserContext.removeUser();
    }

    @Test
    void deletePostRemovesCommentsLikesNotificationsAndTags() {
        UserContext.saveUser(UserDTO.builder().id(1L).build());
        when(postMapper.selectById(10L)).thenReturn(Post.builder().id(10L).userId(1L).build());
        when(commentUserMapper.selectList(any())).thenReturn(List.of(
                Comment.builder().id(100L).rootId(10L).userId(2L).build()));
        when(likeRecordMapper.selectList(any())).thenReturn(
                List.of(LikeRecord.builder().userId(3L).targetType("POST").targetId(10L).build()),
                List.of(LikeRecord.builder().userId(4L).targetType("COMMENT").targetId(100L).build()));
        when(customStringRedisTemplate.opsForSet()).thenReturn(setOperations);

        postService.deletePost(10L);

        verify(likeRecordMapper, times(2)).delete(any());
        verify(commentUserMapper).delete(any());
        verify(notificationMapper, times(2)).delete(any());
        verify(contentTagService).deleteContentTags(10L, "POST");
        verify(postMapper).deleteById(10L);
        verify(setOperations).remove("like:user:3", "POST:10");
        verify(setOperations).remove("like:user:4", "COMMENT:100");
    }
}
