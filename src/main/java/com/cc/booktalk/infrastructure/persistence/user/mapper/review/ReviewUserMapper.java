package com.cc.booktalk.infrastructure.persistence.user.mapper.review;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cc.booktalk.entity.entity.review.BookReview;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 图书评论表 Mapper 接口
 * </p>
 *
 * @author cc
 * @since 2025-06-30
 */
@Mapper
public interface ReviewUserMapper extends BaseMapper<BookReview> {

}
