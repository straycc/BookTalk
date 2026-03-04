package com.cc.booktalk.interfaces.admin.controller;


import com.cc.booktalk.common.result.Result;
import com.cc.booktalk.entity.dto.admin.AdminBookReviewDTO;
import com.cc.booktalk.entity.entity.review.BookReview;
import com.cc.booktalk.application.admin.service.ReviewAdminService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * <p>
 * 图书评论表 前端控制器
 * </p>
 *
 * @author cc
 * @since 2025-07-09
 */
@RestController
@Slf4j
@RequestMapping("/review")
@Api(tags = "评论相关接口")


public class ReviewAdminController {

    @Resource
    private ReviewAdminService reviewAdminService;

    /**
     * 新增单个书评
     * @return
     */
    @ApiOperation("新增单个书评")
    @PostMapping("/books/{bookId}/reviews")
    public Result<Object> reviewAdd(@PathVariable Long bookId,@RequestBody AdminBookReviewDTO adminBookReviewDTO) {
         BookReview bookReview  = reviewAdminService.reviewAdd(bookId,adminBookReviewDTO);
        return Result.success(bookReview);
    }



}
