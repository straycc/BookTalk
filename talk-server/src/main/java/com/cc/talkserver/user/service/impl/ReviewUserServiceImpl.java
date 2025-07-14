package com.cc.talkserver.user.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cc.talkpojo.entity.BookReview;
import com.cc.talkserver.user.mapper.ReviewUserMapper;
import com.cc.talkserver.user.service.ReviewUserService;
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
public class ReviewUserServiceImpl extends ServiceImpl<ReviewUserMapper, BookReview> implements ReviewUserService {

}
