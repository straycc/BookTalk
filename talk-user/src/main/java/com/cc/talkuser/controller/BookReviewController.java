package com.cc.talkuser.controller;


import com.cc.talkcommon.constant.BusinessConstant;
import com.cc.talkcommon.result.Result;
import com.cc.talkpojo.Result.PageResult;
import com.cc.talkpojo.dto.BookReviewDTO;
import com.cc.talkpojo.dto.PageDTO;
import com.cc.talkpojo.dto.PageReviewDTO;
import com.cc.talkpojo.entity.BookReview;
import com.cc.talkpojo.vo.BookReviewVO;
import com.cc.talkserver.user.service.ReviewUserService;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;

/**
 * <p>
 * 图书评论表 前端控制器
 * </p>
 *
 * @author cc
 * @since 2025-06-30
 */
@RestController
@RequestMapping("user/book/review")
public class BookReviewController {

    @Resource
    private ReviewUserService reviewUserService;


    /**
     * 发布书评
     * @param bookReviewDTO
     * @return
     */
    @ApiOperation("发布书评")
    @PostMapping("/publish")
    public Result<Object> addBookReview(@RequestBody BookReviewDTO bookReviewDTO) {
        reviewUserService.publish(bookReviewDTO);
        return Result.success(BusinessConstant.PUBLISH_BOOK_SUCCESS);
    }

    /**
     * 修改书评
     * @param bookReviewId
     * @param bookReviewDTO
     * @return
     */
    @ApiOperation("修改书评")
    @PostMapping("/update/{bookReviewId}")
    public Result<Object> updateBookReview(@PathVariable("bookReviewId") Long bookReviewId, @RequestBody BookReviewDTO bookReviewDTO) {
        reviewUserService.updateBookReview(bookReviewId,bookReviewDTO);
        return Result.success(BusinessConstant.UPDATE_BOOK_SUCCESS);
    }


    /**
     * 删除书评
     * @param bookReviewId
     * @return
     */
    @ApiOperation("删除书评")
    @DeleteMapping("/delete/{bookReviewId}")
    public Result<Object> deleteBookReview(@PathVariable("bookReviewId") Long bookReviewId) {
        reviewUserService.deleteBookReview(bookReviewId);
        return Result.success(BusinessConstant.DELETE_BOOK_SUCCESS);
    }


    /**
     * 查询书籍的书评列表
     * @param bookId
     * @return
     */
    @ApiOperation("查询书籍的书评列表")
    @GetMapping("/list/{bookId}")
    public Result<PageResult<BookReviewVO>> listBookReviews( @PathVariable("bookId") Long bookId, PageReviewDTO pageReviewDTO) {
        PageResult<BookReviewVO> pageResult = reviewUserService.listBookReviews(bookId,pageReviewDTO);
        return Result.success(pageResult);
    }
}
