package com.cc.booktalk.application.user.service.comment.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.cc.booktalk.application.user.service.recommendation.behavior.UserBehaviorEventDispatchService;
import com.cc.booktalk.common.constant.BusinessConstant;
import com.cc.booktalk.common.constant.RedisCacheConstant;
import com.cc.booktalk.common.context.UserContext;
import com.cc.booktalk.common.event.NotificationEventPublisher;
import com.cc.booktalk.common.event.behavior.UserBehaviorEvent;
import com.cc.booktalk.common.event.request.NotificationRequest;
import com.cc.booktalk.common.exception.BaseException;
import com.cc.booktalk.common.utils.CheckPageParam;
import com.cc.booktalk.common.utils.ConvertUtils;
import com.cc.booktalk.common.utils.EnumUtil;
import com.cc.booktalk.interfaces.dto.user.comment.CommentDTO;
import com.cc.booktalk.interfaces.dto.user.UserDTO;
import com.cc.booktalk.common.result.PageResult;
import com.cc.booktalk.interfaces.dto.user.comment.CommentPageDTO;
import com.cc.booktalk.domain.entity.review.BookReview;
import com.cc.booktalk.domain.entity.post.Post;
import com.cc.booktalk.domain.entity.comment.Comment;
import com.cc.booktalk.domain.entity.like.LikeRecord;
import com.cc.booktalk.domain.entity.user.UserInfo;
import com.cc.booktalk.domain.enums.TargetType;
import com.cc.booktalk.interfaces.vo.user.comment.CommentVO;
import com.cc.booktalk.interfaces.vo.user.user.UserVO;
import com.cc.booktalk.infrastructure.persistence.user.mapper.comment.CommentUserMapper;
import com.cc.booktalk.infrastructure.persistence.user.mapper.like.LikeRecordMapper;
import com.cc.booktalk.infrastructure.persistence.user.mapper.post.PostMapper;
import com.cc.booktalk.infrastructure.persistence.user.mapper.review.ReviewUserMapper;
import com.cc.booktalk.infrastructure.persistence.user.mapper.recommendation.UserInfoUserMapper;
import com.cc.booktalk.application.user.service.comment.CommentService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author cc
 * @since 2025-09-08
 */
@Service
@Slf4j
public class CommentServiceImpl extends ServiceImpl<CommentUserMapper, Comment> implements CommentService {

    @Resource
    private CommentUserMapper commentUserMapper;



    // 排序字段映射
    private static final Map<String, SFunction<Comment, ?>> SORT_FIELD_MAP = new HashMap<>();
    static {
        SORT_FIELD_MAP.put("createTime", Comment::getCreateTime);
        SORT_FIELD_MAP.put("id", Comment::getId);
        // 以后需要支持更多字段就加这里
    }

    @Resource
    private RedisTemplate<String,Object> customObjectRedisTemplate;

    @Resource
    private RedisTemplate<String, String> customStringRedisTemplate;

    @Resource
    private ReviewUserMapper reviewUserMapper;
    @Resource
    private PostMapper postMapper;
    @Resource
    private LikeRecordMapper likeRecordMapper;
    @Resource
    private UserInfoUserMapper userInfoUserMapper;
    @Resource
    private NotificationEventPublisher notificationEventPublisher;

    @Resource
    private UserBehaviorEventDispatchService userBehaviorEventDispatchService;


    /**
     * 发表评论
     * @param rootId
     * @param commentDTO
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void commentPublish(Long rootId, CommentDTO commentDTO) {
        if (rootId == null || commentDTO == null || commentDTO.getRootId() == null) {
            throw new BaseException(BusinessConstant.PARAM_ERROR);
        }
        if (!rootId.equals(commentDTO.getRootId())) {
            throw new BaseException(BusinessConstant.PARAM_ERROR);
        }
        if (commentDTO.getContent() == null || commentDTO.getContent().trim().isEmpty()) {
            throw new BaseException(BusinessConstant.COMMENT_ISEMPTY);
        }

        TargetType finalTargetType = EnumUtil.fromCode(TargetType.class, commentDTO.getTargetType());
        Long finalRootId = rootId;

        if (commentDTO.getParentId() != null) {// 评论的评论
            Comment parent = commentUserMapper.selectById(commentDTO.getParentId());
            if (parent == null) {
                throw new BaseException(BusinessConstant.PARENTCOMMENT_NOTEXIST);
            }
            finalRootId = parent.getRootId();
            finalTargetType = parent.getTargetType(); // 继承父评论归属类型
        } else {
            validateRootTarget(finalTargetType, finalRootId);
        }

        Comment comment = Comment.builder()
                .rootId(finalRootId)
                .targetType(finalTargetType)
                .parentId(commentDTO.getParentId())
                .content(commentDTO.getContent())
                .userId(UserContext.getUser().getId())
                .createTime(LocalDateTime.now())
                .build();

        //评论入库
        commentUserMapper.insert(comment);

        // 评论行为埋点（一级评论 / 回复评论）
        publishCommentBehavior(finalTargetType, commentDTO.getParentId(), finalRootId);
        incrementTargetCommentCounter(finalTargetType, finalRootId);

        //消息通知
        publishCommentNotification(comment, finalTargetType);
    }


    /**
     * 删除评论
     * @param commentId
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteComment(Long commentId) {
        if(commentId==null){
            throw new BaseException(BusinessConstant.PARAM_ERROR);
        }

        // 查询评论是否存在
        Comment comment = commentUserMapper.selectById(commentId);
        if(comment==null){
            throw new BaseException(BusinessConstant.COMMENT_NOTEXIST);
        }
        // 查询是否为自己评论
        UserDTO currentUser = UserContext.getUser();
        if (currentUser == null || currentUser.getId() == null) {
            throw new BaseException(BusinessConstant.WITH_NO_AUTHORIZATION);
        }
        Long currentUserId = currentUser.getId();
        if(!currentUserId.equals(comment.getUserId())){
            throw new BaseException(BusinessConstant.DELETE_COMMENT_ERROR);
        }

        List<Comment> commentsInRoot = commentUserMapper.selectList(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getRootId, comment.getRootId())
                .eq(Comment::getTargetType, comment.getTargetType()));
        List<Long> commentIds = collectCommentSubtree(commentId, commentsInRoot);
        if (commentIds.isEmpty()) {
            throw new BaseException(BusinessConstant.COMMENT_NOTEXIST);
        }

        List<LikeRecord> likes = likeRecordMapper.selectList(new LambdaQueryWrapper<LikeRecord>()
                .eq(LikeRecord::getTargetType, BusinessConstant.LIKE_TYPE_COMMENT)
                .in(LikeRecord::getTargetId, commentIds));
        likeRecordMapper.delete(new LambdaQueryWrapper<LikeRecord>()
                .eq(LikeRecord::getTargetType, BusinessConstant.LIKE_TYPE_COMMENT)
                .in(LikeRecord::getTargetId, commentIds));
        commentUserMapper.delete(new LambdaQueryWrapper<Comment>().in(Comment::getId, commentIds));
        decrementTargetCommentCounter(comment.getTargetType(), comment.getRootId(), commentIds.size());
        invalidateCommentLikeCachesAfterCommit(likes, commentIds);
    }


    /**
     * 查询用户所有评论
     * @param userId
     * @param commentPageDTO
     * @return
     */
    @Override
    public PageResult<CommentVO> getUserAllComments(Long userId, CommentPageDTO commentPageDTO) {
        CheckPageParam.checkPageDTO(commentPageDTO);
        if(userId==null  || !userId.equals(commentPageDTO.getUserId())){
            throw new BaseException(BusinessConstant.PARAM_ERROR);
        }

        PageHelper.startPage(commentPageDTO.getPageNum(),commentPageDTO.getPageSize());


        LambdaQueryWrapper<Comment> lambdaQueryWrapper = new LambdaQueryWrapper<>();

        SFunction<Comment, ?> func = SORT_FIELD_MAP.get(commentPageDTO.getSortField());
        if (func != null) {
            boolean isAsc = "asc".equalsIgnoreCase(commentPageDTO.getSortOrder());
            lambdaQueryWrapper.eq(Comment::getUserId, userId).orderBy(true, isAsc, func);
        } else {
            lambdaQueryWrapper.orderByDesc(Comment::getCreateTime);
        }

        List<Comment> listComments = commentUserMapper.selectList(lambdaQueryWrapper);

        //从redis取出用户数据
        UserVO userInfo = loadUserVO(UserContext.getUser().getId());
        if (userInfo == null) {
            throw new BaseException("用户信息不存在或已过期");
        }

        // 转换成VO对象
        List<CommentVO> listCommentVOs = listComments.stream()
                .map(comment -> {
                     CommentVO commentVO = ConvertUtils.convert(comment, CommentVO.class);
                     commentVO.setNickName(userInfo.getNickname());
                     commentVO.setAvatar(userInfo.getAvatarUrl());
                     return commentVO;
                })
                .collect(Collectors.toList());


        PageResult<CommentVO> pageResult = new PageResult<>();
        pageResult.setTotal(listComments.size());
        pageResult.setRecords(listCommentVOs);

        return pageResult;
    }


    /**
     * 查询某个书评所有评论
     * @param bookReviewId
     * @return
     */
    @Override
    public List<CommentVO> bookReviewAllCommments(Long bookReviewId) {
        if(bookReviewId==null){
            throw new BaseException(BusinessConstant.PARAM_ERROR);
        }
        // 判书评是否存在
        BookReview bookReview = reviewUserMapper.selectById(bookReviewId);
        if(bookReview==null){
            throw new BaseException(BusinessConstant.REVIEW_NOTEXIST);
        }

        return targetComments(bookReviewId, TargetType.REVIEW);
    }

    @Override
    public List<CommentVO> postAllComments(Long postId) {
        if (postId == null) {
            throw new BaseException(BusinessConstant.PARAM_ERROR);
        }
        if (postMapper.selectById(postId) == null) {
            throw new BaseException(BusinessConstant.POST_NOTEXIST);
        }
        return targetComments(postId, TargetType.POST);
    }

    private List<CommentVO> targetComments(Long rootId, TargetType targetType) {
        // 查询目标的一级评论
        List<Comment> firstComments = commentUserMapper.selectList(
                new LambdaQueryWrapper<Comment>()
                        .eq(Comment::getRootId, rootId)
                        .eq(Comment::getTargetType, targetType.getCode())
                        .isNull(Comment::getParentId)
                        .orderByDesc(Comment::getCreateTime)
        );

        // 查询所有子评论
        List<Comment> childComments = commentUserMapper.selectList(
                new LambdaQueryWrapper<Comment>()
                        .eq(Comment::getRootId, rootId)
                        .eq(Comment::getTargetType, targetType.getCode())
                        .isNotNull(Comment::getParentId)
                        .orderByAsc(Comment::getCreateTime)
        );


        // 转 VO
        List<CommentVO> allCommentsVO = new ArrayList<>();
        List<CommentVO> firstCommentsVO = firstComments.stream()
                .map(
                        comment ->{
                             CommentVO commentVO = ConvertUtils.convert(comment, CommentVO.class);
                             UserVO userInfo = loadUserVO(commentVO.getUserId());
                            if (userInfo != null) {
                                commentVO.setNickName(userInfo.getNickname());
                                commentVO.setAvatar(userInfo.getAvatarUrl());
                            }

                             return commentVO;
                        })
                .collect(Collectors.toList());

        List<CommentVO> childCommentsVO = childComments.stream()
                .map(comment -> {
                        CommentVO commentVO = ConvertUtils.convert(comment, CommentVO.class);
                        UserVO userInfo = loadUserVO(commentVO.getUserId());
                        if (userInfo != null) {
                            commentVO.setNickName(userInfo.getNickname());
                            commentVO.setAvatar(userInfo.getAvatarUrl());
                        }

                        return commentVO;
                    })
                .collect(Collectors.toList());


        // 用 Map 存子评论 id -> VO，方便挂载
        Map<Long, CommentVO> idToCommentVO = new HashMap<>();
        for (CommentVO vo : firstCommentsVO) {
            idToCommentVO.put(vo.getId(), vo);
        }
        for (CommentVO vo : childCommentsVO) {
            idToCommentVO.put(vo.getId(), vo);
        }

        // 构建树形结构
        for (CommentVO child : childCommentsVO) {
            if (child.getParentId() != null) {
                CommentVO parent = idToCommentVO.get(child.getParentId());
                if (parent != null) {
                    parent.getReplies().add(child);
                }
            }
        }

        // 返回一级评论列表（一级评论下挂载了子评论）
        return firstCommentsVO;
    }

    private UserVO loadUserVO(Long userId) {
        if (userId == null) {
            return null;
        }
        String key = RedisCacheConstant.USER_INFO_KEY_PREFIX + userId;
        Object cached = customObjectRedisTemplate.opsForValue().get(key);
        UserVO userVO = toUserVO(cached);
        if (userVO != null) {
            return userVO;
        }

        UserInfo user = userInfoUserMapper.selectOne(
                new LambdaQueryWrapper<UserInfo>().eq(UserInfo::getUserId, userId)
        );
        if (user == null) {
            return null;
        }
        userVO = ConvertUtils.convert(user, UserVO.class);
        customObjectRedisTemplate.opsForValue().set(key, userVO, 60, TimeUnit.MINUTES);
        return userVO;
    }

    @SuppressWarnings("unchecked")
    private UserVO toUserVO(Object cached) {
        if (cached == null) {
            return null;
        }
        if (cached instanceof UserVO) {
            return (UserVO) cached;
        }
        if (cached instanceof Map) {
            Map<Object, Object> map = (Map<Object, Object>) cached;
            UserVO userVO = new UserVO();
            userVO.setUserId(toLong(map.get("userId")));
            userVO.setNickname(toStringVal(map.get("nickname")));
            userVO.setAvatarUrl(toStringVal(map.get("avatarUrl")));
            return userVO;
        }
        return null;
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (Exception ignore) {
            return null;
        }
    }

    private String toStringVal(Object value) {
        return Objects.toString(value, null);
    }

    /**
     * 发布评论通知
     * @param comment 评论内容
     * @param targetType 目标类型
     */
    private void publishCommentNotification(Comment comment, TargetType targetType) {
        log.info("开始发布评论通知: commentId={}, targetType={}", comment.getId(), targetType);

        try {
            UserDTO currentUser = UserContext.getUser();
            if (currentUser == null) {
                log.warn("当前用户为空，无法发送通知");
                return;
            }

            // 根据不同的目标类型发布通知
            switch (targetType) {
                case REVIEW:
                    // 评论书评，通知书评作者
                    log.info("处理书评评论通知");
                    publishBookReviewCommentNotification(comment, currentUser);
                    break;
                case POST:
                    publishPostCommentNotification(comment, currentUser);
                    break;
                case COMMENT:
                    // 回复评论，通知被回复的评论作者
                    log.info("处理回复评论通知");
                    publishReplyCommentNotification(comment, currentUser);
                    break;
                default:
                    log.warn("不支持的目标类型: {}，跳过通知发送", targetType);
                    break;
            }
        } catch (Exception e) {
            log.error("发布评论通知失败: commentId={}, targetType={}", comment.getId(), targetType, e);
        }
    }

    /**
     * 发布书评评论通知
     * @param comment 评论内容
     * @param currentUser 当前用户
     */
    private void publishBookReviewCommentNotification(Comment comment, UserDTO currentUser) {
        // 查询书评信息
        BookReview bookReview = reviewUserMapper.selectById(comment.getRootId());
        if (bookReview == null || bookReview.getUserId().equals(currentUser.getId())) {
            // 书评不存在或者是自己评论自己的书评，不发送通知
            return;
        }

        // 创建评论通知请求
        NotificationRequest request = NotificationRequest.comment(
                bookReview.getUserId(),               // 接收通知的用户ID（书评作者）
                comment.getRootId(),               // 目标ID（书评ID）
                NotificationRequest.TargetType.REVIEW, // 目标类型
                comment.getContent(),               // 评论内容
                currentUser.getId(),                 // 评论者ID
                currentUser.getNickname(),           // 评论者昵称
                currentUser.getAvatarUrl()              // 评论者头像
        );

        // 发布评论通知
        notificationEventPublisher.publishCommentEvent(request);
    }

    private void publishPostCommentNotification(Comment comment, UserDTO currentUser) {
        Post post = postMapper.selectById(comment.getRootId());
        if (post == null || post.getUserId().equals(currentUser.getId())) {
            return;
        }
        NotificationRequest request = NotificationRequest.comment(
                post.getUserId(),
                comment.getRootId(),
                NotificationRequest.TargetType.POST,
                comment.getContent(),
                currentUser.getId(),
                currentUser.getNickname(),
                currentUser.getAvatarUrl()
        );
        notificationEventPublisher.publishCommentEvent(request);
    }

    /**
     * 发布回复评论通知
     * @param comment 评论内容
     * @param currentUser 当前用户
     */
    private void publishReplyCommentNotification(Comment comment, UserDTO currentUser) {
        log.info("开始处理回复评论通知: commentId={}, parentId={}, currentUser={}",
                comment.getId(), comment.getParentId(), currentUser.getId());

        if (comment.getParentId() == null) {
            log.warn("回复评论没有父评论ID，跳过通知发送: commentId={}", comment.getId());
            return;
        }

        // 查询父评论
        Comment parentComment = commentUserMapper.selectById(comment.getParentId());
        if (parentComment == null) {
            log.warn("找不到父评论，跳过通知发送: parentId={}", comment.getParentId());
            return;
        }

        if (parentComment.getUserId().equals(currentUser.getId())) {
            log.info("回复自己的评论，跳过通知发送: parentId={}, parentUserId={}, currentUserId={}",
                    comment.getParentId(), parentComment.getUserId(), currentUser.getId());
            return;
        }

        log.info("父评论信息: parentId={}, parentUserId={}, 准备发送回复通知",
                parentComment.getId(), parentComment.getUserId());

        try {
            // 创建回复通知请求
            NotificationRequest request = NotificationRequest.builder()
                    .userId(parentComment.getUserId())    // 接收通知的用户ID（被回复的用户）
                    .type(NotificationRequest.NotificationType.REPLY) // 通知类型为回复
                    .targetId(comment.getId())            // 目标ID（当前评论ID）
                    .targetType(NotificationRequest.TargetType.COMMENT) // 目标类型为评论
                    .sender(new NotificationRequest.SenderInfo(
                            currentUser.getId(),          // 回复者ID
                            currentUser.getNickname(),    // 回复者昵称
                            currentUser.getAvatarUrl()       // 回复者头像
                    ))
                    .title("收到新回复")
                    .content(comment.getContent())       // 回复内容
                    .build();

            // 发布回复通知
            notificationEventPublisher.publishReplyEvent(request);
            log.info("回复通知发布成功: commentId={}, 被回复用户={}", comment.getId(), parentComment.getUserId());
        } catch (Exception e) {
            log.error("发布回复通知失败: commentId={}", comment.getId(), e);
        }
    }

    private void validateRootTarget(TargetType targetType, Long rootId) {
        if (targetType == TargetType.REVIEW && reviewUserMapper.selectById(rootId) == null) {
            throw new BaseException(BusinessConstant.REVIEW_NOTEXIST);
        }
        if (targetType == TargetType.POST && postMapper.selectById(rootId) == null) {
            throw new BaseException(BusinessConstant.POST_NOTEXIST);
        }
    }

    private void publishCommentBehavior(TargetType targetType, Long parentId, Long rootId) {
        String behaviorType = resolveCommentBehaviorType(targetType, parentId);
        if (behaviorType == null) {
            return;
        }
        double behaviorScore = parentId == null ? 2.5 : 3.0;
        userBehaviorEventDispatchService.publish(UserBehaviorEvent.builder()
                .userId(UserContext.getUser().getId())
                .targetId(rootId)
                .targetType(targetType.getCode())
                .behaviorType(behaviorType)
                .behaviorScore(behaviorScore)
                .occurredAt(LocalDateTime.now())
                .build());
    }

    private String resolveCommentBehaviorType(TargetType targetType, Long parentId) {
        if (targetType == TargetType.REVIEW) {
            return parentId == null ? "REVIEW_COMMENT" : "REVIEW_REPLY";
        }
        if (targetType == TargetType.POST) {
            return parentId == null ? "POST_COMMENT" : "POST_REPLY";
        }
        return null;
    }

    private void incrementTargetCommentCounter(TargetType targetType, Long rootId) {
        if (targetType == TargetType.REVIEW) {
            reviewUserMapper.update(null, new LambdaUpdateWrapper<BookReview>()
                    .eq(BookReview::getId, rootId)
                    .setSql("reply_count = IFNULL(reply_count, 0) + 1"));
            return;
        }
        if (targetType == TargetType.POST) {
            postMapper.update(null, new LambdaUpdateWrapper<Post>()
                    .eq(Post::getId, rootId)
                    .setSql("comment_count = IFNULL(comment_count, 0) + 1")
                    .set(Post::getLastActiveTime, LocalDateTime.now()));
            return;
        }
    }

    private List<Long> collectCommentSubtree(Long commentId, List<Comment> comments) {
        Map<Long, List<Long>> childrenByParent = comments.stream()
                .filter(comment -> comment.getParentId() != null)
                .collect(Collectors.groupingBy(Comment::getParentId,
                        Collectors.mapping(Comment::getId, Collectors.toList())));
        List<Long> result = new ArrayList<>();
        List<Long> pending = new ArrayList<>();
        pending.add(commentId);
        for (int index = 0; index < pending.size(); index++) {
            Long currentId = pending.get(index);
            result.add(currentId);
            pending.addAll(childrenByParent.getOrDefault(currentId, List.of()));
        }
        return result;
    }

    private void decrementTargetCommentCounter(TargetType targetType, Long rootId, int deletedCount) {
        if (targetType == TargetType.REVIEW) {
            reviewUserMapper.update(null, new LambdaUpdateWrapper<BookReview>()
                    .eq(BookReview::getId, rootId)
                    .setSql("reply_count = GREATEST(IFNULL(reply_count, 0) - " + deletedCount + ", 0)"));
            return;
        }
        if (targetType == TargetType.POST) {
            postMapper.update(null, new LambdaUpdateWrapper<Post>()
                    .eq(Post::getId, rootId)
                    .setSql("comment_count = GREATEST(IFNULL(comment_count, 0) - " + deletedCount + ", 0)")
                    .set(Post::getLastActiveTime, LocalDateTime.now()));
        }
    }

    private void invalidateCommentLikeCachesAfterCommit(List<LikeRecord> likes, List<Long> commentIds) {
        Runnable invalidation = () -> {
            try {
                Map<Long, List<LikeRecord>> likesByComment = likes.stream()
                        .collect(Collectors.groupingBy(LikeRecord::getTargetId));
                for (Long deletedCommentId : commentIds) {
                    String targetField = BusinessConstant.LIKE_TYPE_COMMENT + ':' + deletedCommentId;
                    for (LikeRecord like : likesByComment.getOrDefault(deletedCommentId, List.of())) {
                        customStringRedisTemplate.opsForSet().remove(
                                RedisCacheConstant.LIKE_USER_PREFIX + like.getUserId(), targetField);
                    }
                    customStringRedisTemplate.delete(RedisCacheConstant.LIKE_TARGET_PREFIX + targetField);
                    customStringRedisTemplate.delete(RedisCacheConstant.LIKE_COUNT_PREFIX + targetField);
                }
            } catch (Exception e) {
                log.warn("评论点赞缓存失效失败: commentIds={}", commentIds, e);
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


}
