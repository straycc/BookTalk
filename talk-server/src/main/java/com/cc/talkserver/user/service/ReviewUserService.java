package com.cc.talkserver.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cc.talkpojo.result.PageResult;
import com.cc.talkpojo.dto.BookReviewDTO;
import com.cc.talkpojo.dto.PageReviewDTO;
import com.cc.talkpojo.entity.BookReview;
import com.cc.talkpojo.vo.BookReviewVO;

/**
 * <p>
 * 图书评论表 服务类
 * </p>
 *
 * @author cc
 * @since 2025-06-30
 */
public interface ReviewUserService extends IService<BookReview> {

    /**
     * 发布书评
     * @param bookReviewDTO
     */
    void publish(BookReviewDTO bookReviewDTO);

    /**
     * 修改书评
     * @param bookReviewId
     * @param bookReviewDTO
     */
    void updateBookReview(Long bookReviewId, BookReviewDTO bookReviewDTO);


    /**
     * 删除书评
     * @param bookReviewId
     */
    void deleteBookReview(Long bookReviewId);


    /**
     * 查询书籍的书评列表
     * @param pageReviewDTO
     * @return
     */
    PageResult<BookReviewVO> bookReviewsPage(PageReviewDTO pageReviewDTO);


    /**
     * 查询书评详情
     * @param bookReviewId
     * @return
     */
    BookReviewVO getDetail(Long bookReviewId);
}
