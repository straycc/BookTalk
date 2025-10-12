package com.cc.talkserver.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.cc.talkcommon.constant.BusinessConstant;
import com.cc.talkcommon.constant.RedisCacheConstant;
import com.cc.talkcommon.context.UserContext;
import com.cc.talkcommon.event.NotificationEventPublisher;
import com.cc.talkcommon.event.request.NotificationRequest;
import com.cc.talkcommon.exception.BaseException;
import com.cc.talkcommon.utils.CheckPageParam;
import com.cc.talkcommon.utils.EnumUtil;
import com.cc.talkpojo.dto.UserDTO;
import com.cc.talkpojo.result.PageResult;
import com.cc.talkpojo.dto.LikePageDTO;
import com.cc.talkpojo.dto.LikeRecordDTO;
import com.cc.talkpojo.entity.*;
import com.cc.talkpojo.enums.LikeTargetType;
import com.cc.talkpojo.vo.LikeRecordVO;
import com.cc.talkserver.user.mapper.*;
import com.cc.talkserver.user.service.LikeRecordService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author cc
 * @since 2025-09-10
 */
@Service
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
    private BookListMapper bookListMapper;
    @Resource
    private ReviewUserMapper reviewUserMapper;
    @Resource
    private CommentUserMapper commentUserMapper;

    @Resource
    private NotificationEventPublisher notificationEventPublisher;


    /**
     * 用户点赞
     * @param likeRecordDTO
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clickLike(LikeRecordDTO likeRecordDTO) {

        if(likeRecordDTO == null || likeRecordDTO.getTargetId() == null || likeRecordDTO.getUserId() == null){
            throw  new BaseException(BusinessConstant.PARAM_ERROR);
        }

        // 检查点赞的目标类型
        LikeTargetType targetType;
        try {
            targetType = EnumUtil.fromCode(LikeTargetType.class, likeRecordDTO.getLikeTargetType());
        } catch (IllegalArgumentException e) {
            throw new BaseException(BusinessConstant.TARGETTYPE_ERROR);
        }

        String userKey = RedisCacheConstant.LIKE_USER_PREFIX + likeRecordDTO.getUserId();
        String targetField = likeRecordDTO.getLikeTargetType()+ ':' + likeRecordDTO.getTargetId();
        String targetKey = RedisCacheConstant.LIKE_TARGET_PREFIX + targetField;
        String countKey = RedisCacheConstant.LIKE_COUNT_PREFIX + targetField;
        boolean isLikedNow ;

        if (Boolean.TRUE.equals(customStringRedisTemplate.opsForSet().isMember(userKey, targetField))) {
            // 已点过赞，则取消点赞
            customStringRedisTemplate.opsForSet().remove(userKey, targetField);
            customStringRedisTemplate.opsForSet().remove(targetKey, String.valueOf(likeRecordDTO.getUserId()));
            customStringRedisTemplate.opsForValue().decrement(countKey);
            isLikedNow = false;
        }
        else{
            // 未点赞，则点赞
            customStringRedisTemplate.opsForSet().add(userKey, targetField);
            customStringRedisTemplate.opsForSet().add(targetKey, String.valueOf(likeRecordDTO.getUserId()));
            customStringRedisTemplate.opsForValue().increment(countKey);
            isLikedNow = true;
        }

        // 操作数据库（后期通过MQ异步执行）
        LikeRecord likeRecord = LikeRecord.builder()
                .targetId(likeRecordDTO.getTargetId())
                .targetType(likeRecordDTO.getLikeTargetType())
                .userId(likeRecordDTO.getUserId())
                .createTime(LocalDateTime.now())
                .build();

        if(isLikedNow){
            // 幂等操作，判断数据库是否已存在,再判断是否插入
            Long id = likeRecordMapper.selectByUserTaeget(likeRecord.getUserId(), likeRecord.getTargetType(), likeRecord.getTargetId());
            if(id == null){
                likeRecordMapper.insert(likeRecord);

                // 发布点赞通知
                publishLikeNotification(likeRecordDTO, targetType);
            }
        }
        else{
            likeRecordMapper.deleteByUserAndTarget(likeRecord.getUserId(), likeRecord.getTargetType(), likeRecord.getTargetId());
        }


    }


    /**
     * 查询点赞状态
     * @param likeRecordDTO
     */
    @Override
    public boolean getLikeStatus(LikeRecordDTO likeRecordDTO) {
        if(likeRecordDTO == null || likeRecordDTO.getTargetId() == null || likeRecordDTO.getUserId() == null){
            throw new BaseException(BusinessConstant.PARAM_ERROR);
        }
        String targetField = likeRecordDTO.getLikeTargetType()+ ':' + likeRecordDTO.getTargetId();
        String userKey = RedisCacheConstant.LIKE_USER_PREFIX + likeRecordDTO.getUserId();
        return Boolean.TRUE.equals(customStringRedisTemplate.opsForSet().isMember(userKey, targetField));
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

        String targetField = likeRecordDTO.getLikeTargetType()+ ':' + likeRecordDTO.getTargetId();
        String countKey = RedisCacheConstant.LIKE_COUNT_PREFIX + targetField;
        return Optional.ofNullable(customObjectRedisTemplate.opsForValue().get(countKey))
                .map(v -> Long.parseLong(v.toString()))
                .orElse(0L);
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


        Map<Long, BookList> bookLists = selectBatchIdsToMap(bookListMapper, targetMap.get(BusinessConstant.LIKE_TYPE_BOOKLIST));
        Map<Long, BookReview> reviews = selectBatchIdsToMap(reviewUserMapper, targetMap.get(BusinessConstant.LIKE_TYPE_REVIEW));
        Map<Long, Comment> comments = selectBatchIdsToMap(commentUserMapper, targetMap.get(BusinessConstant.LIKE_TYPE_COMMENT));


        // 收集所有被点赞的用户ID
        Set<Long> targetUserIds = new HashSet<>();
        bookLists.values().forEach(b -> targetUserIds.add(b.getUserId()));
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
                    vo.setNickName(UserContext.getUser().getNickName());
                    vo.setTargetUserAvatar(UserContext.getUser().getAvatar());

                    // 被点赞对象信息
                    Long targetUserId = null;
                    switch (record.getTargetType()) {
                        case BusinessConstant.LIKE_TYPE_BOOKLIST:
                            BookList bookList = bookLists.get(record.getTargetId());
                            if (bookList != null) {
                                vo.setTargetContent(bookList.getTitle());
                                targetUserId = bookList.getUserId();
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
                            vo.setTargetUserAvatar(targetUser.getAvatar());
                        }
                    }

                    // 填充被点赞内容
                    switch (record.getTargetType()) {
                        case BusinessConstant.LIKE_TYPE_BOOKLIST:
                            BookList bookList = bookLists.get(record.getTargetId());
                            if (bookList != null) {
                                vo.setTargetContent(bookList.getTitle());
                            }
                            break;
                        case BusinessConstant.LIKE_TYPE_REVIEW:
                            BookReview review = reviews.get(record.getTargetId());
                            if (review != null) vo.setTargetContent(review.getTitle() == null ? review.getContent() : review.getTitle());
                            break;
                        case BusinessConstant.LIKE_TYPE_COMMENT:
                            Comment comment = comments.get(record.getTargetId());
                            if (comment != null) vo.setTargetContent(comment.getContent());
                            break;
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
            // 获取被点赞内容的作者ID
            Long targetUserId = getTargetUserId(likeRecordDTO.getTargetId(), targetType);
            if (targetUserId == null || targetUserId.equals(likeRecordDTO.getUserId())) {
                // 找不到作者或者是自己给自己点赞，不发送通知
                return;
            }

            // 获取当前用户信息
            UserDTO currentUser = UserContext.getUser();
            if (currentUser == null) {
                return;
            }

            // 转换目标类型
            String notificationTargetType;
            switch (targetType) {
                case BOOK_REVIEW:
                    notificationTargetType = NotificationRequest.TargetType.BOOK_REVIEW;
                    break;
                case COMMENT:
                    notificationTargetType = NotificationRequest.TargetType.COMMENT;
                    break;
                default:
                    // 其他类型暂不发送通知
                    return;
            }

            // 创建点赞通知请求
            NotificationRequest request = NotificationRequest.like(
                    targetUserId,                    // 接收通知的用户ID
                    likeRecordDTO.getTargetId(),     // 被点赞的内容ID
                    notificationTargetType,         // 目标类型
                    currentUser.getId(),             // 点赞者ID
                    currentUser.getNickName(),       // 点赞者昵称
                    currentUser.getAvatar()          // 点赞者头像
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
            case BOOKLIST:
                BookList bookList = bookListMapper.selectById(targetId);
                return bookList != null ? bookList.getUserId() : null;
            default:
                return null;
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
