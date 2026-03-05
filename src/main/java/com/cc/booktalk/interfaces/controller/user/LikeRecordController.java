package com.cc.booktalk.interfaces.controller.user;


import com.cc.booktalk.common.result.Result;
import com.cc.booktalk.common.result.PageResult;
import com.cc.booktalk.infrastructure.aop.annotation.TrackUserBehavior;
import com.cc.booktalk.interfaces.dto.user.like.LikePageDTO;
import com.cc.booktalk.interfaces.dto.user.like.LikeRecordDTO;
import com.cc.booktalk.interfaces.vo.user.like.LikeRecordVO;
import com.cc.booktalk.application.user.service.like.LikeRecordService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author cc
 * @since 2025-09-10
 */
@RestController
@RequestMapping("user/like")
@Api(tags = "点赞相关接口")
public class LikeRecordController {

    @Resource
    private LikeRecordService likeRecordService;


    /**
     * 用户点击点赞（点赞/取消）
     * @param likeRecordDTO
     * @return
     */
    @ApiOperation("用户点击点赞")
    @PostMapping("/likeOrNot")
    public Result<Object> clickLike(@RequestBody LikeRecordDTO likeRecordDTO) {
        // targetType 可选值：bookReview, comment, bookList
        likeRecordService.clickLike(likeRecordDTO);
        return Result.success();
    }

    /**
     * 查询用户点赞状态
     * @param likeRecordDTO
     * @return
     */
    @ApiOperation("查询用户点赞状态")
    @PostMapping("/isLiked")
    public Result<Boolean> isLiked(@RequestBody LikeRecordDTO likeRecordDTO) {
        // 返回当前点赞状态 true/false
        boolean isLike = likeRecordService.getLikeStatus(likeRecordDTO);
        return Result.success(isLike);
    }


    /**
     * 查询目标点赞数量
     * @param targetId
     * @param likeRecordDTO
     * @return
     */
    @ApiOperation("查询目标点赞数量")
    @PostMapping("/count/{targetId}")
    public Result<Long> countLike(@PathVariable Long targetId, @RequestBody LikeRecordDTO likeRecordDTO) {
        Long count = likeRecordService.getLikeCount(targetId,likeRecordDTO);
        return Result.success(count);
    }

    /**
     * 查询用户点赞动态
     */
    @ApiOperation("查询用户点赞动态")
    @PostMapping("/likeDynamic")
    public Result<PageResult<LikeRecordVO>>  likeDynamic(@RequestBody LikePageDTO likePageDTO) {
         PageResult<LikeRecordVO> pageResult = likeRecordService.likeDynamicPage(likePageDTO);
        return Result.success(pageResult);
    }



}
