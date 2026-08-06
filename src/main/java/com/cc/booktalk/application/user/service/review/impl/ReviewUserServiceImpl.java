package com.cc.booktalk.application.user.service.review.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cc.booktalk.application.user.service.content.ContentTagService;
import com.cc.booktalk.common.constant.BusinessConstant;
import com.cc.booktalk.common.constant.RedisCacheConstant;
import com.cc.booktalk.common.context.UserContext;
import com.cc.booktalk.common.exception.BaseException;
import com.cc.booktalk.common.utils.CheckPageParam;
import com.cc.booktalk.common.result.PageResult;
import com.cc.booktalk.interfaces.dto.user.review.BookReviewDTO;
import com.cc.booktalk.interfaces.dto.user.review.PageReviewDTO;
import com.cc.booktalk.domain.entity.review.BookReview;
import com.cc.booktalk.domain.entity.comment.Comment;
import com.cc.booktalk.domain.entity.like.LikeRecord;
import com.cc.booktalk.domain.entity.notification.Notification;
import com.cc.booktalk.domain.entity.user.UserInfo;
import com.cc.booktalk.interfaces.vo.user.review.BookReviewVO;
import com.cc.booktalk.domain.enums.TargetType;
import com.cc.booktalk.infrastructure.persistence.user.mapper.book.BookUserMapper;
import com.cc.booktalk.infrastructure.persistence.user.mapper.comment.CommentUserMapper;
import com.cc.booktalk.infrastructure.persistence.user.mapper.like.LikeRecordMapper;
import com.cc.booktalk.infrastructure.persistence.user.mapper.notification.NotificationMapper;
import com.cc.booktalk.infrastructure.persistence.user.mapper.review.ReviewUserMapper;
import com.cc.booktalk.infrastructure.persistence.user.mapper.recommendation.UserInfoUserMapper;
import com.cc.booktalk.infrastructure.persistence.user.mapper.user.UserMapper;
import com.cc.booktalk.application.user.service.review.ReviewUserService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.stereotype.Service;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 图书评论表 服务实现类
 * </p>
 *
 * @author cc
 * @since 2025-06-30
 */
@Service
@Slf4j
public class ReviewUserServiceImpl extends ServiceImpl<ReviewUserMapper, BookReview> implements ReviewUserService {

    @Resource
    private ReviewUserMapper reviewUserMapper;

    @Resource
    private BookUserMapper bookUserMapper;

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
    @Resource
    private UserMapper userMapper;

    @Resource
    private UserInfoUserMapper userInfoUserMapper;

    @Resource
    private ContentTagService contentTagService;




    /**
     * 发布书评
     * @param bookReviewDTO
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publish(BookReviewDTO bookReviewDTO) {
        // 1.检查参数
        if(bookReviewDTO.getBookId() == null  || bookReviewDTO.getContent() == null || bookReviewDTO.getContent().trim().isEmpty()){
            throw new BaseException(BusinessConstant.PARAM_ERROR);
        }

        // 2. 检查数据库书籍是否存在
        if(bookUserMapper.selectById(bookReviewDTO.getBookId()) == null){
            throw  new BaseException(BusinessConstant.REVIEW_BOOK_NOTEXIST);
        }
        // TODO 2.书评内容关键字屏蔽处理
        // 4.存入数据库
        int type = bookReviewDTO.getType() == null
                ? BusinessConstant.REVIEW_TYPE_SHORT
                : bookReviewDTO.getType();
        if (type != BusinessConstant.REVIEW_TYPE_SHORT && type != BusinessConstant.REVIEW_TYPE_LONG) {
            throw new BaseException(BusinessConstant.REVIEW_TYPE_ERROR);
        }
        if (bookReviewDTO.getScore() != null
                && (bookReviewDTO.getScore() < 1 || bookReviewDTO.getScore() > 10)) {
            throw new BaseException("评分必须在1到10之间");
        }

        // 处理标题（只有长评才需要）
        String title = null;
        if (type == BusinessConstant.REVIEW_TYPE_LONG) {
            title = (bookReviewDTO.getTitle() == null || bookReviewDTO.getTitle().isEmpty())
                    ? BusinessConstant.REVIEW_Title_DEFAULT
                    : bookReviewDTO.getTitle();
        }

        // 构建 BookReview 实体
        BookReview bookReview = BookReview.builder()
                .bookId(bookReviewDTO.getBookId())
                .userId(UserContext.getUser().getId())
                .type(type)
                .title(title) // 短评时 title 可能为 null
                .content(bookReviewDTO.getContent())
                .score(bookReviewDTO.getScore())
                .likeCount(0)
                .replyCount(0)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();

        // 插入数据库
        reviewUserMapper.insert(bookReview);
        contentTagService.replaceContentTags(bookReview.getId(), BusinessConstant.CONTENT_TYPE_REVIEW, bookReviewDTO.getTagIds());
        bookUserMapper.refreshReviewScoreStats(bookReview.getBookId());
    }


    /**
     * 修改书评
     * @param bookReviewId
     * @param bookReviewDTO
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateBookReview(Long bookReviewId, BookReviewDTO bookReviewDTO) {
        if (bookReviewId == null || bookReviewDTO == null || !StringUtils.hasText(bookReviewDTO.getContent())) {
            throw new BaseException(BusinessConstant.PARAM_ERROR);
        }
        // 检查书评是否存在
        BookReview existingReview = reviewUserMapper.selectById(bookReviewId);
        if (existingReview == null) {
            throw new BaseException(BusinessConstant.REVIEW_NOTEXIST);
        }

        // 检查用户权限（是否为自己书评）
        Long currentUserId = UserContext.getUser().getId();
        if (!currentUserId.equals(existingReview.getUserId())) {
            throw new BaseException(BusinessConstant.REVIEW_AUTH_ERROR);
        }

        if (bookReviewDTO.getScore() != null
                && (bookReviewDTO.getScore() < 1 || bookReviewDTO.getScore() > 10)) {
            throw new BaseException("评分必须在1到10之间");
        }

        // 构建 BookReview 实体
        BookReview bookReview = new BookReview();
        bookReview.setId(bookReviewId);
        bookReview.setType(bookReviewDTO.getType());
        bookReview.setTitle(bookReviewDTO.getTitle());
        bookReview.setContent(bookReviewDTO.getContent().trim());
        bookReview.setScore(bookReviewDTO.getScore());
        bookReview.setUpdateTime(LocalDateTime.now());
        reviewUserMapper.updateById(bookReview);
        contentTagService.replaceContentTags(bookReviewId, BusinessConstant.CONTENT_TYPE_REVIEW, bookReviewDTO.getTagIds());
        bookUserMapper.refreshReviewScoreStats(existingReview.getBookId());
    }


    /**
     * 删除书评
     * @param bookReviewId
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBookReview(Long bookReviewId) {
        if (bookReviewId == null || bookReviewId <= 0) {
            throw new BaseException(BusinessConstant.PARAM_ERROR);
        }

        // 检查书评是否存在
        BookReview existingReview = reviewUserMapper.selectById(bookReviewId);
        if (existingReview == null) {
            throw new BaseException(BusinessConstant.REVIEW_NOTEXIST);
        }
        if (!UserContext.getUser().getId().equals(existingReview.getUserId())) {
            throw new BaseException(BusinessConstant.REVIEW_AUTH_ERROR);
        }
        List<Comment> comments = commentUserMapper.selectList(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getRootId, bookReviewId)
                .eq(Comment::getTargetType, TargetType.REVIEW));
        List<Long> commentIds = comments.stream().map(Comment::getId).collect(Collectors.toList());
        List<LikeRecord> reviewLikes = deleteLikes(BusinessConstant.LIKE_TYPE_REVIEW, List.of(bookReviewId));
        List<LikeRecord> commentLikes = deleteLikes(BusinessConstant.LIKE_TYPE_COMMENT, commentIds);
        if (!commentIds.isEmpty()) {
            commentUserMapper.delete(new LambdaQueryWrapper<Comment>().in(Comment::getId, commentIds));
        }
        notificationMapper.delete(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getTargetType, "REVIEW")
                .eq(Notification::getTargetId, bookReviewId));
        if (!commentIds.isEmpty()) {
            notificationMapper.delete(new LambdaQueryWrapper<Notification>()
                    .eq(Notification::getTargetType, "COMMENT")
                    .in(Notification::getTargetId, commentIds));
        }
        contentTagService.deleteContentTags(bookReviewId, BusinessConstant.CONTENT_TYPE_REVIEW);
        reviewUserMapper.deleteById(bookReviewId);
        bookUserMapper.refreshReviewScoreStats(existingReview.getBookId());
        invalidateLikeCachesAfterCommit(reviewLikes, commentLikes, true);
    }

    /**
     * 查询书籍的书评列表
     * @param pageReviewDTO
     * @return
     */
    @Override
    public PageResult<BookReviewVO> bookReviewsPage(PageReviewDTO pageReviewDTO) {
        // 1. 检查参数
        CheckPageParam.checkPageDTO(pageReviewDTO);
        if (pageReviewDTO.getBookId() == null) {
            throw new BaseException(BusinessConstant.PARAM_ERROR);
        }

        // 2. 检查 book 是否存在
        if (bookUserMapper.selectById(pageReviewDTO.getBookId()) == null) {
            throw new BaseException(BusinessConstant.REVIEW_BOOK_NOTEXIST);
        }

        // 3. 分页查询
        PageHelper.startPage(pageReviewDTO.getPageNum(), pageReviewDTO.getPageSize());
        LambdaQueryWrapper<BookReview> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BookReview::getBookId, pageReviewDTO.getBookId());

        if (pageReviewDTO.getType() != null) {
            wrapper.eq(BookReview::getType, pageReviewDTO.getType());
        }
        if (pageReviewDTO.getUserId() != null) {
            wrapper.eq(BookReview::getUserId, pageReviewDTO.getUserId());
        }

        // 排序处理
        if ("asc".equalsIgnoreCase(pageReviewDTO.getSortOrder())) {
            wrapper.orderByAsc(getSortColumn(pageReviewDTO.getSortField()));
        } else {
            wrapper.orderByDesc(getSortColumn(pageReviewDTO.getSortField()));
        }

        List<BookReview> reviewList = reviewUserMapper.selectList(wrapper);
        PageInfo<BookReview> pageInfo = new PageInfo<>(reviewList);

        // 4.1 批量查用户信息，避免把昵称错误地写成当前登录用户
        Set<Long> reviewUserIds = reviewList.stream()
                .map(BookReview::getUserId)
                .collect(Collectors.toSet());
        Map<Long, UserInfo> userInfoMap = reviewUserIds.isEmpty()
                ? Map.of()
                : userInfoUserMapper.selectList(new LambdaQueryWrapper<UserInfo>()
                        .in(UserInfo::getUserId, reviewUserIds))
                .stream()
                .collect(Collectors.toMap(UserInfo::getUserId, it -> it, (a, b) -> a));

        // 4. 转换 VO
        List<BookReviewVO> bookReviewVOList = reviewList.stream()
                .map(review -> {
                    BookReviewVO vo = new BookReviewVO();
                    vo.setBookReviewId(review.getId());
                    vo.setBookId(review.getBookId());
                    vo.setType(review.getType());
                    vo.setTitle(review.getTitle());
                    vo.setContent(review.getContent());
                    vo.setScore(review.getScore());
                    vo.setLikeCount(review.getLikeCount());
                    vo.setUserId(review.getUserId());
                    vo.setCreateTime(review.getCreateTime());
                    vo.setUpdateTime(review.getUpdateTime());

                    // 查询评论数
                    LambdaQueryWrapper<Comment> commentWrapper = new LambdaQueryWrapper<>();
                    commentWrapper.eq(Comment::getRootId, review.getId())
                                  .eq(Comment::getTargetType, TargetType.REVIEW);
                    Long commentCount = commentUserMapper.selectCount(commentWrapper);
                    vo.setCommentCount(commentCount.intValue());

                    // 查用户信息（按书评作者 userId 回填）
                    UserInfo userInfo = userInfoMap.get(review.getUserId());
                    if (userInfo != null) {
                        vo.setNickName(userInfo.getNickname());
                        vo.setAvatarUrl(userInfo.getAvatarUrl());
                    }
                    vo.setTags(contentTagService.getTagsByContent(review.getId(), BusinessConstant.CONTENT_TYPE_REVIEW));
                    return vo;
                })
                .collect(Collectors.toList());

        // 5. 返回结果
        PageResult<BookReviewVO> pageResult = new PageResult<>();
        pageResult.setTotal(pageInfo.getTotal());
        pageResult.setRecords(bookReviewVOList);
        return pageResult;
    }


    /**
     * 查询书评详情
     * @param bookReviewId
     * @return
     */
    @Override
    public BookReviewVO getDetail(Long bookReviewId) {
        if(bookReviewId == null || bookReviewId <= 0){
            throw new BaseException(BusinessConstant.PARAM_ERROR);
        }
        BookReview bookReview = reviewUserMapper.selectById(bookReviewId);
        if (bookReview == null) {
            throw new BaseException(BusinessConstant.REVIEW_NOTEXIST);
        }
        BookReviewVO bookReviewVO = new BookReviewVO();
        bookReviewVO.setBookReviewId(bookReviewId);
        BeanUtil.copyProperties(bookReview, bookReviewVO);

        // 查询评论数
        LambdaQueryWrapper<Comment> commentWrapper = new LambdaQueryWrapper<>();
        commentWrapper.eq(Comment::getRootId, bookReviewId)
                      .eq(Comment::getTargetType, TargetType.REVIEW);
        Long commentCount = commentUserMapper.selectCount(commentWrapper);
        bookReviewVO.setCommentCount(commentCount.intValue());

        // 查询书评用户信息
        UserInfo userInfo = userInfoUserMapper.selectOne(
                new LambdaQueryWrapper<UserInfo>().eq(UserInfo::getUserId,bookReview.getUserId())
        );
        bookReviewVO.setUserId(bookReview.getUserId());
        if(userInfo != null){
            bookReviewVO.setNickName(userInfo.getNickname());
            bookReviewVO.setAvatarUrl(userInfo.getAvatarUrl());
        }
        bookReviewVO.setTags(contentTagService.getTagsByContent(bookReview.getId(), BusinessConstant.CONTENT_TYPE_REVIEW));
        return bookReviewVO;
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

    private void invalidateLikeCachesAfterCommit(List<LikeRecord> reviewLikes, List<LikeRecord> commentLikes,
                                                 boolean invalidateReviewRanking) {
        Runnable invalidation = () -> {
            try {
                List<LikeRecord> likes = new java.util.ArrayList<>();
                likes.addAll(reviewLikes);
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
                if (invalidateReviewRanking) {
                    customObjectRedisTemplate.delete(RedisCacheConstant.RANKING_HOT_REVIEWS_PREFIX + "weekly");
                }
            } catch (Exception e) {
                log.warn("删除书评后的点赞缓存失效失败", e);
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


    /**
     * 获取排序字段对应的列
     */
    private SFunction<BookReview, ?> getSortColumn(String sortField) {
        if ("score".equalsIgnoreCase(sortField)) {
            return BookReview::getScore;
        } else if ("likeCount".equalsIgnoreCase(sortField)) {
            return BookReview::getLikeCount;
        } else {
            return BookReview::getCreateTime; // 默认按创建时间
        }
    }


}
