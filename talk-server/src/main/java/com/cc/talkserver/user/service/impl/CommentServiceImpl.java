package com.cc.talkserver.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.cc.talkcommon.constant.BusinessConstant;
import com.cc.talkcommon.constant.RedisCacheConstant;
import com.cc.talkcommon.context.UserContext;
import com.cc.talkcommon.exception.BaseException;
import com.cc.talkcommon.utils.CheckPageParam;
import com.cc.talkcommon.utils.ConvertUtils;
import com.cc.talkcommon.utils.EnumUtil;
import com.cc.talkpojo.dto.CommentDTO;
import com.cc.talkpojo.result.PageResult;
import com.cc.talkpojo.dto.CommentPageDTO;
import com.cc.talkpojo.entity.BookReview;
import com.cc.talkpojo.entity.Comment;
import com.cc.talkpojo.entity.UserInfo;
import com.cc.talkpojo.enums.TargetType;
import com.cc.talkpojo.vo.CommentVO;
import com.cc.talkpojo.vo.UserVO;
import com.cc.talkserver.user.mapper.CommentUserMapper;
import com.cc.talkserver.user.mapper.ReviewUserMapper;
import com.cc.talkserver.user.mapper.UserInfoUserMapper;
import com.cc.talkserver.user.service.CommentService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author cc
 * @since 2025-09-08
 */
@Service
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
    private ReviewUserMapper reviewUserMapper;
    @Resource
    private UserInfoUserMapper userInfoUserMapper;


    /**
     * 发表评论
     * @param targetId
     * @param commentDTO
     */
    @Override
    public void commentPublish(Long targetId, CommentDTO commentDTO) {

        // 检查参数
        if(targetId == null || commentDTO == null || !targetId.equals(commentDTO.getTargetId())){
            throw new BaseException(BusinessConstant.PARAM_ERROR);
        }
        if (commentDTO.getContent() == null || commentDTO.getContent().trim().isEmpty()) {
            throw new BaseException(BusinessConstant.COMMENT_ISEMPTY);
        }

        // 检查目标枚举类型,并获取目标枚举值
         TargetType targetType = EnumUtil.fromCode(TargetType.class,commentDTO.getTargetType());

        // 检查父级评论（为空：直接评论书评， 不为空：是否存在）
        if(commentDTO.getParentId()!=null){
            Comment  parent = commentUserMapper.selectById(commentDTO.getParentId());
            if(parent==null){
                throw new BaseException(BusinessConstant.PARENTCOMMENT_NOTEXIST);
            }
        }

        //存入数据仓库
        Comment comment = Comment.builder()
                .targetId(targetId)
                .content(commentDTO.getContent())
                .targetType(targetType)
                .parentId(commentDTO.getParentId())
                .userId(UserContext.getUser().getId())
                .createTime(LocalDateTime.now())
                .build();

        commentUserMapper.insert(comment);
    }


    /**
     * 删除评论
     * @param commentId
     */
    @Override
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
        Long currentUserId = UserContext.getUser().getId();
        if(!currentUserId.equals(comment.getUserId())){
            throw new BaseException(BusinessConstant.DELETE_COMMENT_ERROR);
        }
        commentUserMapper.deleteById(commentId);
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
        String key = RedisCacheConstant.USER_INFO_KEY_PREFIX + UserContext.getUser().getId();
        UserVO userInfo = (UserVO) customObjectRedisTemplate.opsForValue().get(key);
        if (userInfo == null) {
            throw new BaseException("用户信息不存在或已过期");
        }

        // 转换成VO对象
        List<CommentVO> listCommentVOs = listComments.stream()
                .map(comment -> {
                     CommentVO commentVO = ConvertUtils.convert(comment, CommentVO.class);
                     commentVO.setNickName(userInfo.getNickname());
                     commentVO.setAvatar(userInfo.getAvatar());
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

        // 查询书评的一级评论
        List<Comment> firstComments = commentUserMapper.selectList(
                new LambdaQueryWrapper<Comment>()
                        .eq(Comment::getTargetId, bookReviewId)
                        .eq(Comment::getTargetType, TargetType.BOOKREVIEW.getCode())
                        .isNull(Comment::getParentId)
                        .orderByDesc(Comment::getCreateTime)
        );

        // 查询所有子评论
        List<Comment> childComments = commentUserMapper.selectList(
                new LambdaQueryWrapper<Comment>()
                        .eq(Comment::getTargetId, bookReviewId)
                        .eq(Comment::getTargetType, TargetType.BOOKREVIEW.getCode())
                        .isNotNull(Comment::getParentId)
                        .orderByAsc(Comment::getCreateTime)
        );


        // 转 VO
        List<CommentVO> allCommentsVO = new ArrayList<>();
        List<CommentVO> firstCommentsVO = firstComments.stream()
                .map(
                        comment ->{
                             CommentVO commentVO = ConvertUtils.convert(comment, CommentVO.class);
                             String key = RedisCacheConstant.USER_INFO_KEY_PREFIX + commentVO.getUserId();
                             UserVO userInfo = (UserVO) customObjectRedisTemplate.opsForValue().get(key);
                             if (userInfo == null) {
                                 UserInfo user = userInfoUserMapper.selectOne(
                                         new LambdaQueryWrapper<UserInfo>().eq(UserInfo::getUserId, commentVO.getUserId())
                                 );
                                 if (user != null) {
                                     userInfo = ConvertUtils.convert(user, UserVO.class);

                                     //  缓存到 Redis，设置过期时间，比如 1 小时
                                     customObjectRedisTemplate.opsForValue().set(key, userInfo, 60, TimeUnit.MINUTES);
                                 }
                             }
                            if (userInfo != null) {
                                commentVO.setNickName(userInfo.getNickname());
                                commentVO.setAvatar(userInfo.getAvatar());
                            }

                             return commentVO;
                        })
                .collect(Collectors.toList());

        List<CommentVO> childCommentsVO = childComments.stream()
                .map(comment -> {
                        CommentVO commentVO = ConvertUtils.convert(comment, CommentVO.class);
                        String key = RedisCacheConstant.USER_INFO_KEY_PREFIX + commentVO.getUserId();
                        UserVO userInfo = (UserVO) customObjectRedisTemplate.opsForValue().get(key);
                        if (userInfo == null) {
                            UserInfo user = userInfoUserMapper.selectOne(
                                    new LambdaQueryWrapper<UserInfo>().eq(UserInfo::getUserId, commentVO.getUserId())
                            );
                            if (user != null) {
                                userInfo = ConvertUtils.convert(user, UserVO.class);

                                //  缓存到 Redis，设置过期时间，比如 1 小时
                                customObjectRedisTemplate.opsForValue().set(key, userInfo, 60, TimeUnit.MINUTES);
                            }
                        }
                        if (userInfo != null) {
                            commentVO.setNickName(userInfo.getNickname());
                            commentVO.setAvatar(userInfo.getAvatar());
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


}
