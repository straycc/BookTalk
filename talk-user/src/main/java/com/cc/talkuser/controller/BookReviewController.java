package com.cc.talkuser.controller;
import com.cc.talkcommon.constant.BusinessConstant;
import com.cc.talkcommon.result.Result;
import com.cc.talkpojo.result.PageResult;
import com.cc.talkpojo.dto.BookReviewDTO;
import com.cc.talkpojo.dto.PageReviewDTO;
import com.cc.talkpojo.vo.BookReviewVO;
import com.cc.talkserver.user.service.ReviewUserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
@RequestMapping("user/bookReview")
@Api(tags = "书评相关接口")
public class BookReviewController {

    private static final Logger log = LoggerFactory.getLogger(BookReviewController.class);
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
     * @return
     */
    @ApiOperation("查询书籍的书评列表")
    @GetMapping("/page")
    public Result<PageResult<BookReviewVO>> bookReviewsPage(PageReviewDTO pageReviewDTO) {
        PageResult<BookReviewVO> pageResult = reviewUserService.bookReviewsPage(pageReviewDTO);
        return Result.success(pageResult);
    }

    /**
     * 查询书评详情
     * @param bookReviewId
     * @return
     */
    @ApiOperation("查询书评详情")
    @GetMapping("/detail/{bookReviewId}")
    public Result<BookReviewVO> bookReviewDetail(@PathVariable("bookReviewId") Long bookReviewId) {
        log.info("查询书评详情：{}", bookReviewId);
        BookReviewVO bookReviewVO  = reviewUserService.getDetail(bookReviewId);
        return Result.success(bookReviewVO);
    }


}
