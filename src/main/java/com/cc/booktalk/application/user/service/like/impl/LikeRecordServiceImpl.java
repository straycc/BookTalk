package com.cc.booktalk.application.user.service.like.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.cc.booktalk.application.user.service.recommendation.behavior.UserBehaviorEventDispatchService;
import com.cc.booktalk.common.constant.BusinessConstant;
import com.cc.booktalk.common.constant.RedisCacheConstant;
import com.cc.booktalk.common.context.UserContext;
import com.cc.booktalk.common.event.NotificationEventPublisher;
import com.cc.booktalk.common.event.behavior.UserBehaviorEvent;
import com.cc.booktalk.common.event.request.NotificationRequest;
import com.cc.booktalk.common.exception.BaseException;
import com.cc.booktalk.common.utils.CheckPageParam;
import com.cc.booktalk.common.utils.EnumUtil;
import com.cc.booktalk.interfaces.dto.user.UserDTO;
import com.cc.booktalk.domain.entity.comment.Comment;
import com.cc.booktalk.domain.entity.like.LikeRecord;
import com.cc.booktalk.domain.entity.post.Post;
import com.cc.booktalk.domain.entity.review.BookReview;
import com.cc.booktalk.domain.entity.user.UserInfo;
import com.cc.booktalk.common.result.PageResult;
import com.cc.booktalk.interfaces.dto.user.like.LikePageDTO;
import com.cc.booktalk.interfaces.dto.user.like.LikeRecordDTO;
import com.cc.booktalk.domain.enums.LikeTargetType;
import com.cc.booktalk.interfaces.vo.user.like.LikeRecordVO;
import com.cc.booktalk.infrastructure.persistence.user.mapper.comment.CommentUserMapper;
import com.cc.booktalk.infrastructure.persistence.user.mapper.like.LikeRecordMapper;
import com.cc.booktalk.infrastructure.persistence.user.mapper.post.PostMapper;
import com.cc.booktalk.infrastructure.persistence.user.mapper.recommendation.UserInfoUserMapper;
import com.cc.booktalk.infrastructure.persistence.user.mapper.review.ReviewUserMapper;
import com.cc.booktalk.infrastructure.persistence.user.mapper.book.BookUserMapper;
import com.cc.booktalk.application.user.service.like.LikeRecordService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author cc
 * @since 2025-09-10
 */
@Service
@Slf4j
public class LikeRecordServiceImpl extends ServiceImpl<LikeRecordMapper, LikeRecord> implements LikeRecordService {

    @Resource
    private LikeRecordMapper likeRecordMapper;

    @Resource
    private RedisTemplate<String, String> customStringRedisTemplate;

    @Resource
    private RedisTemplate<String, Object> customObjectRedisTemplate;

    @Resource
    private UserInfoUserMapper userInfoUserMapper;

    // 用于查询点赞内容
    @Resource
    private BookUserMapper bookUserMapper;
    @Resource
    private ReviewUserMapper reviewUserMapper;
    @Resource
    private PostMapper postMapper;
    @Resource
    private CommentUserMapper commentUserMapper;

    @Resource
    private NotificationEventPublisher notificationEventPublisher;

    @Resource
    private UserBehaviorEventDispatchService userBehaviorEventDispatchService;


    /**
     * 用户点赞
     * @param likeRecordDTO
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clickLike(LikeRecordDTO likeRecordDTO) {
        UserDTO currentUser = UserContext.getUser();
        Long currentUserId = currentUser == null ? null : currentUser.getId();
        if(likeRecordDTO == null || likeRecordDTO.getTargetId() == null || currentUserId == null){
            throw  new BaseException(BusinessConstant.PARAM_ERROR);
        }

        // 检查点赞的目标类型
        LikeTargetType targetType;
        try {
            targetType = EnumUtil.fromCode(LikeTargetType.class, likeRecordDTO.getLikeTargetType());
        } catch (IllegalArgumentException e) {
            throw new BaseException(BusinessConstant.TARGETTYPE_ERROR);
        }

        // 数据库是点赞状态的唯一事实来源，Redis 仅在事务提交后同步。
        LikeRecord likeRecord = LikeRecord.builder()
                .targetId(likeRecordDTO.getTargetId())
                .targetType(targetType.getCode())
                .userId(currentUserId)
                .createTime(LocalDateTime.now())
                .build();
        Long existingLikeId = likeRecordMapper.selectByUserTaeget(
                likeRecord.getUserId(), likeRecord.getTargetType(), likeRecord.getTargetId());
        boolean isLikedNow = existingLikeId == null;
        if (isLikedNow) {
            if (likeRecordMapper.insert(likeRecord) != 1) {
                throw new BaseException("点赞失败");
            }
            incrementLikeCounter(targetType, likeRecord.getTargetId(), 1);
        } else {
            int deleted = likeRecordMapper.deleteByUserAndTarget(
                    likeRecord.getUserId(), likeRecord.getTargetType(), likeRecord.getTargetId());
            if (deleted > 0) {
                incrementLikeCounter(targetType, likeRecord.getTargetId(), -1);
            }
        }

        long likeCount = likeRecordMapper.selectCount(new LambdaQueryWrapper<LikeRecord>()
                .eq(LikeRecord::getTargetType, likeRecord.getTargetType())
                .eq(LikeRecord::getTargetId, likeRecord.getTargetId()));
        synchronizeLikeCacheAfterCommit(currentUserId, targetType.getCode(), likeRecord.getTargetId(),
                isLikedNow, likeCount);

        if (isLikedNow) {
            publishLikeBehavior(true, currentUserId, likeRecordDTO, targetType);
            publishLikeNotification(likeRecordDTO, targetType);
        }
    }

    /**
     * 查询点赞状态
     * @param likeRecordDTO
     */
    @Override
    public boolean getLikeStatus(LikeRecordDTO likeRecordDTO) {
        UserDTO currentUser = UserContext.getUser();
        Long currentUserId = currentUser == null ? null : currentUser.getId();
        if(likeRecordDTO == null || likeRecordDTO.getTargetId() == null || currentUserId == null){
            throw new BaseException(BusinessConstant.PARAM_ERROR);
        }
        LikeTargetType targetType = checkTargetType(likeRecordDTO.getLikeTargetType());
        String targetField = targetType.getCode() + ':' + likeRecordDTO.getTargetId();
        String userKey = RedisCacheConstant.LIKE_USER_PREFIX + currentUserId;
        if (Boolean.TRUE.equals(customStringRedisTemplate.opsForSet().isMember(userKey, targetField))) {
            return true;
        }
        boolean liked = likeRecordMapper.selectByUserTaeget(currentUserId, targetType.getCode(),
                likeRecordDTO.getTargetId()) != null;
        if (liked) {
            customStringRedisTemplate.opsForSet().add(userKey, targetField);
            customStringRedisTemplate.opsForSet().add(
                    RedisCacheConstant.LIKE_TARGET_PREFIX + targetField, String.valueOf(currentUserId));
        }
        return liked;
    }


    /**
     * 查询目标点赞数量
     * @param targetId
     * @param likeRecordDTO
     * @return
     */
    @Override
    public Long getLikeCount(Long targetId, LikeRecordDTO likeRecordDTO) {

        if(likeRecordDTO == null || likeRecordDTO.getTargetId() == null ||
                !likeRecordDTO.getTargetId().equals(targetId) || likeRecordDTO.getLikeTargetType() == null){
            throw new BaseException(BusinessConstant.PARAM_ERROR);
        }
        LikeTargetType targetType = checkTargetType(likeRecordDTO.getLikeTargetType());

        String targetField = targetType.getCode() + ':' + likeRecordDTO.getTargetId();
        String countKey = RedisCacheConstant.LIKE_COUNT_PREFIX + targetField;
        Object cachedCount = customObjectRedisTemplate.opsForValue().get(countKey);
        if (cachedCount != null) {
            try {
                return Long.parseLong(cachedCount.toString());
            } catch (NumberFormatException ignored) {
                customObjectRedisTemplate.delete(countKey);
            }
        }
        long count = likeRecordMapper.selectCount(new LambdaQueryWrapper<LikeRecord>()
                .eq(LikeRecord::getTargetType, targetType.getCode())
                .eq(LikeRecord::getTargetId, targetId));
        customObjectRedisTemplate.opsForValue().set(countKey, count);
        return count;
    }

    private void synchronizeLikeCacheAfterCommit(Long userId, String targetType, Long targetId,
                                                  boolean liked, long likeCount) {
        Runnable synchronization = () -> {
            try {
                String targetField = targetType + ':' + targetId;
                String userKey = RedisCacheConstant.LIKE_USER_PREFIX + userId;
                String targetKey = RedisCacheConstant.LIKE_TARGET_PREFIX + targetField;
                if (liked) {
                    customStringRedisTemplate.opsForSet().add(userKey, targetField);
                    customStringRedisTemplate.opsForSet().add(targetKey, String.valueOf(userId));
                } else {
                    customStringRedisTemplate.opsForSet().remove(userKey, targetField);
                    customStringRedisTemplate.opsForSet().remove(targetKey, String.valueOf(userId));
                }
                customObjectRedisTemplate.opsForValue().set(
                        RedisCacheConstant.LIKE_COUNT_PREFIX + targetField, likeCount);
            } catch (Exception e) {
                log.warn("点赞缓存同步失败: userId={}, targetType={}, targetId={}", userId, targetType, targetId, e);
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    synchronization.run();
                }
            });
            return;
        }
        synchronization.run();
    }


    /**
     * 查询用户点赞动态
     * @param likePageDTO
     * @return
     */
    @Override
    public PageResult<LikeRecordVO> likeDynamicPage(LikePageDTO likePageDTO) {
        CheckPageParam.checkPageDTO(likePageDTO);
        if(likePageDTO.getUserId() == null ){
            return new PageResult<>();
        }
        PageHelper.startPage(likePageDTO.getPageNum(), likePageDTO.getPageSize());
        // 查询参数
        LambdaQueryWrapper<LikeRecord> queryWrapper = new LambdaQueryWrapper<LikeRecord>();
        queryWrapper.eq(LikeRecord::getUserId, likePageDTO.getUserId());
        queryWrapper.orderByDesc(LikeRecord::getCreateTime);

        List<LikeRecord> likeRecords = likeRecordMapper.selectList(queryWrapper);
        if(likeRecords.isEmpty()){
            return new PageResult<>();
        }


        // 收集 targetId 按类型批量查询
        Map<String, List<Long>> targetMap = likeRecords.stream()
                .collect(Collectors.groupingBy(LikeRecord::getTargetType,
                        Collectors.mapping(LikeRecord::getTargetId, Collectors.toList())));


        Map<Long, Post> posts = selectBatchIdsToMap(postMapper, targetMap.get(BusinessConstant.LIKE_TYPE_POST));
        Map<Long, BookReview> reviews = selectBatchIdsToMap(reviewUserMapper, targetMap.get(BusinessConstant.LIKE_TYPE_REVIEW));
        Map<Long, Comment> comments = selectBatchIdsToMap(commentUserMapper, targetMap.get(BusinessConstant.LIKE_TYPE_COMMENT));


        // 收集所有被点赞的用户ID
        Set<Long> targetUserIds = new HashSet<>();
        posts.values().forEach(p -> targetUserIds.add(p.getUserId()));
        reviews.values().forEach(r -> targetUserIds.add(r.getUserId()));
        comments.values().forEach(c -> targetUserIds.add(c.getUserId()));

        // 查询被点赞用户信息
        List<UserInfo> userInfos = userInfoUserMapper.selectList(
                Wrappers.<UserInfo>lambdaQuery().in(UserInfo::getUserId, targetUserIds)
        );
        Map<Long, UserInfo> targetUserInfoMap = userInfos.stream()
                .collect(Collectors.toMap(UserInfo::getUserId, u -> u));


        // 填充 VO
        List<LikeRecordVO> likeRecordVOs = likeRecords.stream().map(record -> {
                    LikeRecordVO vo = new LikeRecordVO();
                    vo.setTargetId(record.getTargetId());
                    vo.setTargetType(record.getTargetType());
                    vo.setCreateTime(record.getCreateTime());

                    // 点赞者信息
                    vo.setLikeUserId(UserContext.getUser().getId());
                    vo.setNickName(UserContext.getUser().getNickname());
                    vo.setTargetUserAvatar(UserContext.getUser().getAvatarUrl());

                    // 被点赞对象信息
                    Long targetUserId = null;
                    switch (record.getTargetType()) {
                        case BusinessConstant.LIKE_TYPE_POST:
                            Post post = posts.get(record.getTargetId());
                            if (post != null) {
                                vo.setTargetContent(post.getTitle());
                                targetUserId = post.getUserId();
                            }
                            break;
                        case BusinessConstant.LIKE_TYPE_REVIEW:
                            BookReview review = reviews.get(record.getTargetId());
                            if (review != null) {
                                vo.setTargetContent(review.getTitle() == null ? review.getContent() : review.getTitle());
                                targetUserId = review.getUserId();
                            }
                            break;
                        case BusinessConstant.LIKE_TYPE_COMMENT:
                            Comment comment = comments.get(record.getTargetId());
                            if (comment != null) {
                                vo.setTargetContent(comment.getContent());
                                targetUserId = comment.getUserId();
                            }
                            break;
                    }

                    if (targetUserId != null) {
                        UserInfo targetUser = targetUserInfoMap.get(targetUserId);
                        if (targetUser != null) {
                            vo.setTargetUserId(targetUser.getUserId());
                            vo.setTargetNickName(targetUser.getNickname());
                            vo.setTargetUserAvatar(targetUser.getAvatarUrl());
                        }
                    }

                    return vo;
        }).collect(Collectors.toList());

        PageInfo<LikeRecord> pageInfo = new PageInfo<>(likeRecords);

        PageResult<LikeRecordVO> pageResult = new PageResult<>();
        pageResult.setRecords(likeRecordVOs);
        pageResult.setTotal(pageInfo.getTotal()); // 用 PageHelper 的总数
        return pageResult;

    }


    /**
     * 检查点赞目标类型
     * @param type
     */
    public LikeTargetType checkTargetType(String type) {
        // 检查点赞的目标类型
        LikeTargetType targetType;
        try {
            targetType = EnumUtil.fromCode(LikeTargetType.class, type);
            return  targetType;
        } catch (IllegalArgumentException e) {
            throw new BaseException(BusinessConstant.TARGETTYPE_ERROR);
        }
    }



    /**
     * 发布点赞通知
     * @param likeRecordDTO 点赞记录
     * @param targetType 目标类型
     */
    private void publishLikeNotification(LikeRecordDTO likeRecordDTO, LikeTargetType targetType) {
        try {
            UserDTO currentUser = UserContext.getUser();
            if (currentUser == null || currentUser.getId() == null) {
                return;
            }
            // 获取被点赞内容的作者ID
            Long targetUserId = getTargetUserId(likeRecordDTO.getTargetId(), targetType);
            if (targetUserId == null || targetUserId.equals(currentUser.getId())) {
                // 找不到作者或者是自己给自己点赞，不发送通知
                return;
            }

            // 转换目标类型
            String notificationTargetType = null;
            switch (targetType) {
                case BOOK_REVIEW:
                    notificationTargetType = NotificationRequest.TargetType.REVIEW;
                    break;
                case POST:
                    notificationTargetType = NotificationRequest.TargetType.POST;
                    break;
                case COMMENT:
                    notificationTargetType = NotificationRequest.TargetType.COMMENT;
                    break;
            }
            if (notificationTargetType == null) {
                return;
            }

            // 创建点赞通知请求
            NotificationRequest request = NotificationRequest.like(
                    targetUserId,                    // 接收通知的用户ID
                    likeRecordDTO.getTargetId(),     // 被点赞的内容ID
                    notificationTargetType,         // 目标类型
                    currentUser.getId(),             // 点赞者ID
                    currentUser.getNickname(),       // 点赞者昵称
                    currentUser.getAvatarUrl()          // 点赞者头像
            );

            // 发布通知
            notificationEventPublisher.publishLikeEvent(request);
        } catch (Exception e) {
            // 通知发送失败不影响主流程
            // 可以考虑记录日志
        }
    }

    /**
     * 获取被点赞内容的作者ID
     * @param targetId 目标ID
     * @param targetType 目标类型
     * @return 作者ID
     */
    private Long getTargetUserId(Long targetId, LikeTargetType targetType) {
        switch (targetType) {
            case BOOK_REVIEW:
                BookReview review = reviewUserMapper.selectById(targetId);
                return review != null ? review.getUserId() : null;
            case COMMENT:
                Comment comment = commentUserMapper.selectById(targetId);
                return comment != null ? comment.getUserId() : null;
            case POST:
                Post post = postMapper.selectById(targetId);
                return post != null ? post.getUserId() : null;
            default:
                return null;
        }
    }

    private void publishLikeBehavior(boolean isLikedNow, Long currentUserId, LikeRecordDTO likeRecordDTO, LikeTargetType targetType) {
        if (!isLikedNow) {
            return;
        }
        String behaviorType = null;
        String targetTypeCode = null;
        if (targetType == LikeTargetType.BOOK_REVIEW) {
            behaviorType = "REVIEW_LIKE";
            targetTypeCode = "REVIEW";
        } else if (targetType == LikeTargetType.POST) {
            behaviorType = "POST_LIKE";
            targetTypeCode = "POST";
        }
        if (behaviorType == null) {
            return;
        }
        UserBehaviorEvent behaviorEvent = UserBehaviorEvent.builder()
                .userId(currentUserId)
                .targetId(likeRecordDTO.getTargetId())
                .targetType(targetTypeCode)
                .behaviorType(behaviorType)
                .behaviorScore(2.0)
                .occurredAt(LocalDateTime.now())
                .build();
        userBehaviorEventDispatchService.publish(behaviorEvent);
    }

    private void incrementLikeCounter(LikeTargetType targetType, Long targetId, int delta) {
        if (targetType == LikeTargetType.BOOK_REVIEW) {
            reviewUserMapper.update(null, new LambdaUpdateWrapper<BookReview>()
                    .eq(BookReview::getId, targetId)
                    .setSql("like_count = GREATEST(IFNULL(like_count, 0) + (" + delta + "), 0)"));
            return;
        }
        if (targetType == LikeTargetType.POST) {
            postMapper.update(null, new LambdaUpdateWrapper<Post>()
                    .eq(Post::getId, targetId)
                    .setSql("like_count = GREATEST(IFNULL(like_count, 0) + (" + delta + "), 0)"));
            return;
        }
    }

    /**
     * 根据 ID 列表批量查询并转换成 Map
     *
     * @param mapper 批量查询 Mapper
     * @param ids    ID 列表
     * @param <T>    实体类型
     * @return Map<id, entity>
     */
    public static <T> Map<Long, T> selectBatchIdsToMap(BaseMapper<T> mapper, List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return mapper.selectBatchIds(ids)
                .stream()
                .collect(Collectors.toMap(entity -> {
                    try {
                        // 假设每个实体都有 getId() 方法
                        return (Long) entity.getClass().getMethod("getId").invoke(entity);
                    } catch (Exception e) {
                        throw new RuntimeException("获取实体ID失败", e);
                    }
                }, t -> t));
    }

}
