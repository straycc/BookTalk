package com.cc.talkuser.controller;


import com.cc.talkcommon.constant.BusinessConstant;
import com.cc.talkcommon.result.Result;
import com.cc.talkpojo.dto.CommentDTO;
import com.cc.talkpojo.result.PageResult;
import com.cc.talkpojo.dto.CommentPageDTO;
import com.cc.talkpojo.vo.CommentVO;
import com.cc.talkserver.user.service.CommentService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author cc
 * @since 2025-09-08
 */
@RestController
@RequestMapping("/user/comment")
@Api(tags = "评论相关接口")
public class CommentController {

    @Resource
    private CommentService commentService;


    @ApiOperation("发布评论")
    @PostMapping("/publish/{targetId}")
    public Result<Object> publish(@PathVariable("targetId") Long targetId, @RequestBody CommentDTO commentDTO) {
        commentService.commentPublish(targetId,commentDTO);
        return Result.success(BusinessConstant.PUBLISH_COMMENT_SUCCESS);
    }

    @ApiOperation("删除评论")
    @PostMapping("/delete/{commentId}")
    public Result<Object> deleteComment(@PathVariable("commentId") Long commentId) {
        commentService.deleteComment(commentId);
        return Result.success(BusinessConstant.DELETE_COMMENT_SUCCESS);
    }

    @ApiOperation("查询用户所有评论")
    @PostMapping("/user/{userId}")
    public Result<PageResult<CommentVO>> userAllComments(@PathVariable("userId") Long userId,
                                          @RequestBody CommentPageDTO commentPageDTO) {
        PageResult<CommentVO> pageResult = commentService.getUserAllComments(userId, commentPageDTO);
        return Result.success(pageResult);
    }

    @ApiOperation("查询书评所有评论")
    @GetMapping("/bookReview/{bookReviewId}")
    public Result<Object> bookReviewAllComments(@PathVariable("bookReviewId") Long bookReviewId) {
        List<CommentVO> commentVOList = commentService.bookReviewAllCommments(bookReviewId);
        return Result.success(commentVOList);
    }
}

