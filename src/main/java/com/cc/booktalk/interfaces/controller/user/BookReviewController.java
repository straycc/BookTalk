package com.cc.booktalk.interfaces.controller.user;
import com.cc.booktalk.common.constant.BusinessConstant;
import com.cc.booktalk.common.result.Result;
import com.cc.booktalk.common.result.PageResult;
import com.cc.booktalk.infrastructure.aop.annotation.TrackUserBehavior;
import com.cc.booktalk.interfaces.dto.user.review.BookReviewDTO;
import com.cc.booktalk.interfaces.dto.user.review.PageReviewDTO;
import com.cc.booktalk.interfaces.vo.user.review.BookReviewVO;
import com.cc.booktalk.application.user.service.review.ReviewUserService;
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
    @TrackUserBehavior(
        behaviorType = "BOOK_REVIEW",
        targetType = "BOOK",
        targetIdParam = "bookReviewDTO.bookId",
        behaviorScore = 4.0
    )
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
