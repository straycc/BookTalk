package com.cc.talkuser.controller;


import com.cc.talkcommon.result.Result;
import com.cc.talkpojo.result.PageResult;
import com.cc.talkpojo.dto.LikePageDTO;
import com.cc.talkpojo.dto.LikeRecordDTO;
import com.cc.talkpojo.vo.LikeRecordVO;
import com.cc.talkserver.user.service.LikeRecordService;
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
