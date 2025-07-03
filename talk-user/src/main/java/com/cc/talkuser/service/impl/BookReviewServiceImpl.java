package com.cc.talkuser.service.impl;

import com.cc.talkpojo.entity.BookReview;
import com.cc.talkuser.mapper.BookReviewMapper;
import com.cc.talkuser.service.IBookReviewService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 图书评论表 服务实现类
 * </p>
 *
 * @author cc
 * @since 2025-06-30
 */
@Service
public class BookReviewServiceImpl extends ServiceImpl<BookReviewMapper, BookReview> implements IBookReviewService {

}
