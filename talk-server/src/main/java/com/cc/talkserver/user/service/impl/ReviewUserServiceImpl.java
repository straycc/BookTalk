package com.cc.talkserver.user.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cc.talkcommon.constant.BusinessConstant;
import com.cc.talkcommon.context.UserContext;
import com.cc.talkcommon.exception.BaseException;
import com.cc.talkcommon.utils.CheckPageParam;
import com.cc.talkpojo.result.PageResult;
import com.cc.talkpojo.dto.BookReviewDTO;
import com.cc.talkpojo.dto.PageReviewDTO;
import com.cc.talkpojo.entity.BookReview;
import com.cc.talkpojo.entity.UserInfo;
import com.cc.talkpojo.vo.BookReviewVO;
import com.cc.talkserver.user.mapper.BookUserMapper;
import com.cc.talkserver.user.mapper.ReviewUserMapper;
import com.cc.talkserver.user.mapper.UserInfoUserMapper;
import com.cc.talkserver.user.mapper.UserMapper;
import com.cc.talkserver.user.service.ReviewUserService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.time.LocalDateTime;
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
public class ReviewUserServiceImpl extends ServiceImpl<ReviewUserMapper, BookReview> implements ReviewUserService {

    @Resource
    private ReviewUserMapper reviewUserMapper;

    @Resource
    private BookUserMapper bookUserMapper;
    @Resource
    private UserMapper userMapper;

    @Resource
    private UserInfoUserMapper userInfoUserMapper;




    /**
     * 发布书评
     * @param bookReviewDTO
     */
    @Override
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
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();

        // 插入数据库
        reviewUserMapper.insert(bookReview);
    }


    /**
     * 修改书评
     * @param bookReviewId
     * @param bookReviewDTO
     */
    @Override
    public void updateBookReview(Long bookReviewId, BookReviewDTO bookReviewDTO) {
        if (bookReviewId == null || bookReviewDTO == null || bookReviewDTO.getBookId() == null
                || bookReviewDTO.getUserId() == null || !StringUtils.hasText(bookReviewDTO.getContent())) {
            throw new BaseException(BusinessConstant.PARAM_ERROR);
        }
        // 检查书评是否存在
        BookReview existingReview = reviewUserMapper.selectById(bookReviewId);
        if (existingReview == null) {
            throw new BaseException(BusinessConstant.REVIEW_NOTEXIST);
        }

        // 检查用户权限（是否为自己书评）
        Long currentUserId = UserContext.getUser().getId();
        if(!currentUserId.equals(bookReviewDTO.getUserId())){
            throw new BaseException(BusinessConstant.REVIEW_AUTH_ERROR);
        }

        // 构建 BookReview 实体
        BookReview bookReview = new BookReview();
        BeanUtil.copyProperties(bookReviewDTO, bookReview);
        reviewUserMapper.updateById(bookReview);
    }


    /**
     * 删除书评
     * @param bookReviewId
     */
    @Override
    public void deleteBookReview(Long bookReviewId) {
        if (bookReviewId == null || bookReviewId <= 0) {
            throw new BaseException(BusinessConstant.PARAM_ERROR);
        }

        // 检查书评是否存在
        BookReview existingReview = reviewUserMapper.selectById(bookReviewId);
        if (existingReview == null) {
            throw new BaseException(BusinessConstant.REVIEW_NOTEXIST);
        }
        reviewUserMapper.deleteById(bookReviewId);
        // TODO 删除redis/ES 书评内容
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

                    // 查用户信息
                    vo.setUsername(UserContext.getUser().getUsername());
//                    User user = userMapper.selectById(review.getUserId());
//                    if (user != null) {
//                        vo.setUsername(user.getUsername());
//                        vo.setAvatar(user.getAvatar()); // 头像
//                    }
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
        BookReviewVO bookReviewVO = new BookReviewVO();
        BeanUtil.copyProperties(bookReview, bookReviewVO);
        // 查询书评用户信息
        UserInfo userInfo = userInfoUserMapper.selectById(bookReview.getUserId());
        bookReviewVO.setUserId(userInfo.getUserId());
        bookReviewVO.setUsername(userInfo.getNickname());
        bookReviewVO.setAvatarUrl(userInfo.getAvatar());

        return bookReviewVO;
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
