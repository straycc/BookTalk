package com.cc.booktalk.application.user.service.post.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.cc.booktalk.application.user.service.content.ContentTagService;
import com.cc.booktalk.application.user.service.post.PostService;
import com.cc.booktalk.common.constant.BusinessConstant;
import com.cc.booktalk.common.constant.RedisCacheConstant;
import com.cc.booktalk.common.context.UserContext;
import com.cc.booktalk.common.exception.BaseException;
import com.cc.booktalk.common.result.PageResult;
import com.cc.booktalk.common.utils.CheckPageParam;
import com.cc.booktalk.domain.entity.book.Book;
import com.cc.booktalk.domain.entity.post.Post;
import com.cc.booktalk.domain.entity.comment.Comment;
import com.cc.booktalk.domain.entity.like.LikeRecord;
import com.cc.booktalk.domain.entity.notification.Notification;
import com.cc.booktalk.domain.entity.user.UserInfo;
import com.cc.booktalk.interfaces.dto.user.post.PostCreateDTO;
import com.cc.booktalk.interfaces.dto.user.post.PostPageDTO;
import com.cc.booktalk.interfaces.vo.user.post.PostVO;
import com.cc.booktalk.infrastructure.persistence.user.mapper.book.BookUserMapper;
import com.cc.booktalk.infrastructure.persistence.user.mapper.post.PostMapper;
import com.cc.booktalk.infrastructure.persistence.user.mapper.comment.CommentUserMapper;
import com.cc.booktalk.infrastructure.persistence.user.mapper.like.LikeRecordMapper;
import com.cc.booktalk.infrastructure.persistence.user.mapper.notification.NotificationMapper;
import com.cc.booktalk.infrastructure.persistence.user.mapper.recommendation.UserInfoUserMapper;
import org.springframework.stereotype.Service;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class PostServiceImpl implements PostService {

    @Resource
    private PostMapper postMapper;

    @Resource
    private UserInfoUserMapper userInfoUserMapper;

    @Resource
    private BookUserMapper bookUserMapper;

    @Resource
    private ContentTagService contentTagService;
    @Resource
    private CommentUserMapper commentUserMapper;
    @Resource
    private LikeRecordMapper likeRecordMapper;
    @Resource
    private NotificationMapper notificationMapper;
    @Resource
    private RedisTemplate<String, String> customStringRedisTemplate;
    @Resource
    private RedisTemplate<String, Object> customObjectRedisTemplate;

    @Override
    public Long createPost(PostCreateDTO postCreateDTO) {
        if (postCreateDTO == null) {
            throw new BaseException(BusinessConstant.PARAM_ERROR);
        }
        if (postCreateDTO.getTitle() == null || postCreateDTO.getTitle().trim().isEmpty()) {
            throw new BaseException(BusinessConstant.POST_TITLE_EMPTY);
        }
        if (postCreateDTO.getContent() == null || postCreateDTO.getContent().trim().isEmpty()) {
            throw new BaseException(BusinessConstant.POST_CONTENT_EMPTY);
        }
        if (postCreateDTO.getRelatedBookId() != null && bookUserMapper.selectById(postCreateDTO.getRelatedBookId()) == null) {
            throw new BaseException(BusinessConstant.BOOK_NOTEXIST);
        }
        LocalDateTime now = LocalDateTime.now();
        Post post = Post.builder()
                .userId(UserContext.getUser().getId())
                .title(postCreateDTO.getTitle().trim())
                .content(postCreateDTO.getContent().trim())
                .summary(buildSummary(postCreateDTO.getSummary(), postCreateDTO.getContent()))
                .relatedBookId(postCreateDTO.getRelatedBookId())
                .viewCount(0)
                .likeCount(0)
                .commentCount(0)
                .hotScore(0.0)
                .status(1)
                .lastActiveTime(now)
                .hotScoreUpdateTime(now)
                .createTime(now)
                .updateTime(now)
                .build();
        postMapper.insert(post);
        contentTagService.replaceContentTags(post.getId(), BusinessConstant.CONTENT_TYPE_POST, postCreateDTO.getTagIds());
        return post.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePost(Long postId) {
        if (postId == null || postId <= 0 || UserContext.getUser() == null || UserContext.getUser().getId() == null) {
            throw new BaseException(BusinessConstant.PARAM_ERROR);
        }
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new BaseException(BusinessConstant.POST_NOTEXIST);
        }
        if (!UserContext.getUser().getId().equals(post.getUserId())) {
            throw new BaseException(BusinessConstant.WITH_NO_AUTHORIZATION);
        }

        List<Comment> comments = commentUserMapper.selectList(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getRootId, postId)
                .eq(Comment::getTargetType, "POST"));
        List<Long> commentIds = comments.stream().map(Comment::getId).collect(Collectors.toList());
        List<LikeRecord> postLikes = deleteLikes(BusinessConstant.LIKE_TYPE_POST, List.of(postId));
        List<LikeRecord> commentLikes = deleteLikes(BusinessConstant.LIKE_TYPE_COMMENT, commentIds);
        if (!commentIds.isEmpty()) {
            commentUserMapper.delete(new LambdaQueryWrapper<Comment>().in(Comment::getId, commentIds));
        }
        notificationMapper.delete(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getTargetType, "POST")
                .eq(Notification::getTargetId, postId));
        if (!commentIds.isEmpty()) {
            notificationMapper.delete(new LambdaQueryWrapper<Notification>()
                    .eq(Notification::getTargetType, "COMMENT")
                    .in(Notification::getTargetId, commentIds));
        }
        contentTagService.deleteContentTags(postId, BusinessConstant.CONTENT_TYPE_POST);
        postMapper.deleteById(postId);
        invalidateLikeCachesAfterCommit(postLikes, commentLikes);
    }

    @Override
    public PostVO getPostDetail(Long postId) {
        if (postId == null) {
            throw new BaseException(BusinessConstant.PARAM_ERROR);
        }
        Post post = postMapper.selectById(postId);
        if (post == null || post.getStatus() == null || post.getStatus() != 1) {
            throw new BaseException(BusinessConstant.POST_NOTEXIST);
        }
        postMapper.update(null, new LambdaUpdateWrapper<Post>()
                .eq(Post::getId, postId)
                .setSql("view_count = IFNULL(view_count, 0) + 1"));
        post.setViewCount((post.getViewCount() == null ? 0 : post.getViewCount()) + 1);
        return buildPostVO(post, loadUserMap(List.of(post.getUserId())), loadBookMap(post.getRelatedBookId() == null ? List.of() : List.of(post.getRelatedBookId())));
    }

    @Override
    public PageResult<PostVO> getPostPage(PostPageDTO postPageDTO) {
        CheckPageParam.checkPageDTO(postPageDTO);
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Post::getStatus, 1);
        if (BusinessConstant.POST_SORT_HOT.equalsIgnoreCase(postPageDTO.getSort())) {
            wrapper.orderByDesc(Post::getHotScore).orderByDesc(Post::getLastActiveTime).orderByDesc(Post::getCreateTime);
        } else {
            wrapper.orderByDesc(Post::getLastActiveTime).orderByDesc(Post::getCreateTime);
        }
        List<Post> posts = postMapper.selectList(wrapper);
        if (postPageDTO.getTagId() != null) {
            Set<Long> allowedIds = contentTagService.getTagsByContents(
                    posts.stream().map(Post::getId).collect(Collectors.toList()),
                    BusinessConstant.CONTENT_TYPE_POST
            ).entrySet().stream()
                    .filter(entry -> entry.getValue().stream().anyMatch(tag -> postPageDTO.getTagId().equals(tag.getId())))
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toSet());
            posts = posts.stream().filter(post -> allowedIds.contains(post.getId())).collect(Collectors.toList());
        }

        Map<Long, UserInfo> userMap = loadUserMap(posts.stream().map(Post::getUserId).collect(Collectors.toList()));
        Map<Long, Book> bookMap = loadBookMap(posts.stream().map(Post::getRelatedBookId).filter(id -> id != null).collect(Collectors.toList()));
        List<PostVO> records = posts.stream()
                .map(post -> buildPostVO(post, userMap, bookMap))
                .collect(Collectors.toList());
        return paginate(records, postPageDTO.getPageNum(), postPageDTO.getPageSize());
    }

    private PostVO buildPostVO(Post post, Map<Long, UserInfo> userMap, Map<Long, Book> bookMap) {
        PostVO postVO = new PostVO();
        postVO.setId(post.getId());
        postVO.setUserId(post.getUserId());
        postVO.setTitle(post.getTitle());
        postVO.setContent(post.getContent());
        postVO.setSummary(post.getSummary());
        postVO.setRelatedBookId(post.getRelatedBookId());
        postVO.setViewCount(defaultInt(post.getViewCount()));
        postVO.setLikeCount(defaultInt(post.getLikeCount()));
        postVO.setCommentCount(defaultInt(post.getCommentCount()));
        postVO.setHotScore(post.getHotScore() == null ? 0.0 : post.getHotScore());
        postVO.setLastActiveTime(post.getLastActiveTime());
        postVO.setCreateTime(post.getCreateTime());
        postVO.setTags(contentTagService.getTagsByContent(post.getId(), BusinessConstant.CONTENT_TYPE_POST));

        UserInfo userInfo = userMap.get(post.getUserId());
        if (userInfo != null) {
            postVO.setAuthorName(userInfo.getNickname());
            postVO.setAuthorAvatar(userInfo.getAvatarUrl());
        }
        if (post.getRelatedBookId() != null) {
            Book book = bookMap.get(post.getRelatedBookId());
            if (book != null) {
                postVO.setRelatedBookName(book.getTitle());
                postVO.setRelatedBookCover(book.getCoverUrl());
            }
        }
        return postVO;
    }

    private Map<Long, UserInfo> loadUserMap(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        return userInfoUserMapper.selectList(new LambdaQueryWrapper<UserInfo>()
                        .in(UserInfo::getUserId, userIds.stream().filter(id -> id != null).collect(Collectors.toSet())))
                .stream()
                .collect(Collectors.toMap(UserInfo::getUserId, item -> item, (left, right) -> left));
    }

    private Map<Long, Book> loadBookMap(List<Long> bookIds) {
        if (bookIds == null || bookIds.isEmpty()) {
            return Map.of();
        }
        return bookUserMapper.selectBatchIds(bookIds.stream().filter(id -> id != null).collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(Book::getId, item -> item, (left, right) -> left));
    }

    private String buildSummary(String summary, String content) {
        if (summary != null && !summary.trim().isEmpty()) {
            return summary.trim();
        }
        String normalized = content == null ? "" : content.trim();
        if (normalized.length() <= 80) {
            return normalized;
        }
        return normalized.substring(0, 80) + "...";
    }

    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }

    private List<LikeRecord> deleteLikes(String targetType, List<Long> targetIds) {
        if (targetIds == null || targetIds.isEmpty()) {
            return List.of();
        }
        List<LikeRecord> likes = likeRecordMapper.selectList(new LambdaQueryWrapper<LikeRecord>()
                .eq(LikeRecord::getTargetType, targetType)
                .in(LikeRecord::getTargetId, targetIds));
        likeRecordMapper.delete(new LambdaQueryWrapper<LikeRecord>()
                .eq(LikeRecord::getTargetType, targetType)
                .in(LikeRecord::getTargetId, targetIds));
        return likes;
    }

    private void invalidateLikeCachesAfterCommit(List<LikeRecord> postLikes, List<LikeRecord> commentLikes) {
        Runnable invalidation = () -> {
            try {
                List<LikeRecord> likes = new java.util.ArrayList<>();
                likes.addAll(postLikes);
                likes.addAll(commentLikes);
                Map<String, List<LikeRecord>> likesByTarget = likes.stream().collect(Collectors.groupingBy(
                        like -> like.getTargetType() + ':' + like.getTargetId()));
                for (Map.Entry<String, List<LikeRecord>> entry : likesByTarget.entrySet()) {
                    for (LikeRecord like : entry.getValue()) {
                        customStringRedisTemplate.opsForSet().remove(
                                RedisCacheConstant.LIKE_USER_PREFIX + like.getUserId(), entry.getKey());
                    }
                    customStringRedisTemplate.delete(RedisCacheConstant.LIKE_TARGET_PREFIX + entry.getKey());
                    customStringRedisTemplate.delete(RedisCacheConstant.LIKE_COUNT_PREFIX + entry.getKey());
                }
            } catch (Exception e) {
                log.warn("删除帖子后的点赞缓存失效失败", e);
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    invalidation.run();
                }
            });
            return;
        }
        invalidation.run();
    }

    private <T> PageResult<T> paginate(List<T> items, int pageNum, int pageSize) {
        int fromIndex = Math.min((pageNum - 1) * pageSize, items.size());
        int toIndex = Math.min(fromIndex + pageSize, items.size());
        return new PageResult<>(items.size(), items.subList(fromIndex, toIndex));
    }
}
