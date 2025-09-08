package com.cc.talkserver.admin.service.impl;

import com.cc.talkcommon.constant.BusinessConstant;
import com.cc.talkcommon.constant.RedisCacheConstant;
import com.cc.talkcommon.context.UserContext;
import com.cc.talkcommon.exception.BaseException;
import com.cc.talkpojo.dto.admin.AdminBookReviewDTO;
import com.cc.talkpojo.entity.Book;
import com.cc.talkpojo.entity.BookReview;
import com.cc.talkpojo.enums.ReviewType;
import com.cc.talkserver.admin.mapper.BookAdminMapper;
import com.cc.talkserver.admin.mapper.ReviewAdminMapper;
import com.cc.talkserver.admin.service.ReviewAdminService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;

/**
 * <p>
 * 图书评论表 服务实现类
 * </p>
 *
 * @author cc
 * @since 2025-08-18
 */
@Service
public class ReviewAdminServiceImpl extends ServiceImpl<ReviewAdminMapper, BookReview> implements ReviewAdminService {


    @Resource
    private RedisTemplate<String, String> customStringRedisTemplate;

    @Resource
    private RedisTemplate<String, Object> customObjectRedisTemplate;

    @Resource
    private ReviewAdminMapper reviewAdminMapper;
    @Autowired
    private BookAdminMapper bookAdminMapper;


    /**
     * 新增单个书评
     * @param bookId
     * @param adminBookReviewDTO
     * @return
     */
    @Override
    public BookReview reviewAdd(Long bookId, AdminBookReviewDTO adminBookReviewDTO) {
        // 1. 参数校验
        if(bookId == null || adminBookReviewDTO == null || !bookId.equals(adminBookReviewDTO.getBookId())){
            throw new BaseException(BusinessConstant.PARAM_ERROR);
        }
        // 查询redis，判断书籍是否存在
        String hashKey = RedisCacheConstant.BOOK_DETAIL_KEY_PREFIX;
        String fieldKey = String.valueOf(bookId);

        Object bookObj = customObjectRedisTemplate.opsForHash().get(hashKey, fieldKey);
        if (bookObj == null || bookObj.equals(RedisCacheConstant.CACHE_BLANK)) {
            // 如果redis中没有书籍数据，则查询数据库
            Book book = bookAdminMapper.selectById(bookId);
            if (book == null) {
                throw new BaseException(BusinessConstant.BOOK_NOTEXIST);
            }
        }

        // 书评类型校验
        boolean flag  = ReviewType.isValid(adminBookReviewDTO.getType());
        if(!flag){
            throw new BaseException(BusinessConstant.REVIEW_TYPE_ERROR);
        }
        //2. 评论写入mysql
        // TODO 暂时从DTO 获取UserID
        // Long userId = UserContext.getUser().getId();
        Long userId = adminBookReviewDTO.getUserId();
        BookReview bookReview = BookReview.builder()
                .bookId(bookId)
                .userId(userId)
                .type(adminBookReviewDTO.getType())
                .title(adminBookReviewDTO.getTitle())
                .score(adminBookReviewDTO.getScore())
                .content(adminBookReviewDTO.getContent())
                .status(BusinessConstant.REVIEW_WAIT_AUDIT)
                .createTime(adminBookReviewDTO.getCreateTime()) // TODO 暂时使用DTO
                .build();
        reviewAdminMapper.insert(bookReview);
        // TODO 定时任务查询热门书评写入ES 和 redis
        return null;
    }
}
