package com.cc.talkserver.admin.service;

import com.cc.talkpojo.dto.admin.AdminBookReviewDTO;
import com.cc.talkpojo.entity.BookReview;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 图书评论表 服务类
 * </p>
 *
 * @author cc
 * @since 2025-08-18
 */
public interface ReviewAdminService extends IService<BookReview> {

    /**
     * 新增单个书评
     * @param bookId
     * @param adminBookReviewDTO
     * @return
     */
    BookReview reviewAdd(Long bookId, AdminBookReviewDTO adminBookReviewDTO);
}
