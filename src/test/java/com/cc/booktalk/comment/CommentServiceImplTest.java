package com.cc.booktalk.comment;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.cc.booktalk.application.user.service.comment.impl.CommentServiceImpl;
import com.cc.booktalk.application.user.service.recommendation.behavior.UserBehaviorEventDispatchService;
import com.cc.booktalk.common.context.UserContext;
import com.cc.booktalk.common.event.NotificationEventPublisher;
import com.cc.booktalk.common.exception.BaseException;
import com.cc.booktalk.domain.entity.comment.Comment;
import com.cc.booktalk.domain.entity.like.LikeRecord;
import com.cc.booktalk.domain.entity.post.Post;
import com.cc.booktalk.domain.entity.review.BookReview;
import com.cc.booktalk.domain.enums.TargetType;
import com.cc.booktalk.infrastructure.persistence.user.mapper.comment.CommentUserMapper;
import com.cc.booktalk.infrastructure.persistence.user.mapper.like.LikeRecordMapper;
import com.cc.booktalk.infrastructure.persistence.user.mapper.post.PostMapper;
import com.cc.booktalk.infrastructure.persistence.user.mapper.recommendation.UserInfoUserMapper;
import com.cc.booktalk.infrastructure.persistence.user.mapper.review.ReviewUserMapper;
import com.cc.booktalk.interfaces.dto.user.UserDTO;
import com.cc.booktalk.interfaces.dto.user.comment.CommentDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentServiceImplTest {

    @InjectMocks
    private CommentServiceImpl commentService;

    @Mock
    private CommentUserMapper commentUserMapper;
    @Mock
    private LikeRecordMapper likeRecordMapper;
    @Mock
    private ReviewUserMapper reviewUserMapper;
    @Mock
    private PostMapper postMapper;
    @Mock
    private UserInfoUserMapper userInfoUserMapper;
    @Mock
    private NotificationEventPublisher notificationEventPublisher;
    @Mock
    private UserBehaviorEventDispatchService userBehaviorEventDispatchService;
    @Mock
    private RedisTemplate<String, Object> customObjectRedisTemplate;
    @Mock
    private RedisTemplate<String, String> customStringRedisTemplate;
    @Mock
    private SetOperations<String, String> setOperations;

    @AfterEach
    void clearUserContext() {
        UserContext.removeUser();
    }

    @Test
    void publishReviewCommentIncrementsReplyCount() {
        loginAs(1L);
        CommentDTO dto = new CommentDTO();
        dto.setRootId(10L);
        dto.setTargetType(TargetType.REVIEW.getCode());
        dto.setContent("comment");
        when(reviewUserMapper.selectById(10L)).thenReturn(BookReview.builder().id(10L).userId(2L).build());

        commentService.commentPublish(10L, dto);

        verify(commentUserMapper).insert(any(Comment.class));
        verify(reviewUserMapper).update(any(), any(Wrapper.class));
        verify(userBehaviorEventDispatchService).publish(any());
    }

    @Test
    void deleteCommentDeletesRepliesLikesAndDecrementsRootCounter() {
        loginAs(1L);
        Comment root = comment(100L, null, 1L);
        Comment child = comment(101L, 100L, 2L);
        Comment grandchild = comment(102L, 101L, 3L);
        when(commentUserMapper.selectById(100L)).thenReturn(root);
        when(commentUserMapper.selectList(any())).thenReturn(List.of(root, child, grandchild));
        when(likeRecordMapper.selectList(any())).thenReturn(List.of(
                LikeRecord.builder().userId(9L).targetType("COMMENT").targetId(101L).build()));
        when(customStringRedisTemplate.opsForSet()).thenReturn(setOperations);

        commentService.deleteComment(100L);

        verify(likeRecordMapper).delete(any());
        verify(commentUserMapper).delete(any());
        verify(reviewUserMapper).update(any(), any(Wrapper.class));
        verify(setOperations).remove("like:user:9", "COMMENT:101");
        verify(customStringRedisTemplate).delete("like:target:COMMENT:100");
        verify(customStringRedisTemplate).delete("like:count:COMMENT:102");
    }

    @Test
    void deleteMissingCommentFailsWithoutMutatingData() {
        loginAs(1L);
        when(commentUserMapper.selectById(100L)).thenReturn(null);

        BaseException exception = assertThrows(BaseException.class, () -> commentService.deleteComment(100L));

        assertEquals("评论不存在", exception.getMsg());
        verify(commentUserMapper, never()).delete(any());
        verify(likeRecordMapper, never()).delete(any());
        verify(reviewUserMapper, never()).update(any(), any());
    }

    @Test
    void deleteAnotherUsersCommentFailsWithoutMutatingData() {
        loginAs(1L);
        when(commentUserMapper.selectById(100L)).thenReturn(comment(100L, null, 2L));

        BaseException exception = assertThrows(BaseException.class, () -> commentService.deleteComment(100L));

        assertEquals("仅支持删除自己的评论", exception.getMsg());
        verify(commentUserMapper, never()).delete(any());
        verify(likeRecordMapper, never()).delete(any());
        verify(reviewUserMapper, never()).update(any(), any());
    }

    @Test
    void getPostCommentsReturnsPostCommentTree() {
        when(postMapper.selectById(20L)).thenReturn(Post.builder().id(20L).status(1).build());
        when(commentUserMapper.selectList(any())).thenReturn(List.of());

        assertTrue(commentService.postAllComments(20L).isEmpty());

        verify(commentUserMapper, times(2)).selectList(any());
    }

    private void loginAs(Long userId) {
        UserContext.saveUser(UserDTO.builder().id(userId).nickname("tester").build());
    }

    private Comment comment(Long id, Long parentId, Long userId) {
        return Comment.builder()
                .id(id)
                .rootId(10L)
                .targetType(TargetType.REVIEW)
                .parentId(parentId)
                .userId(userId)
                .content("comment")
                .build();
    }
}
