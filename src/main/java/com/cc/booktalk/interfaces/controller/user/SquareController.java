package com.cc.booktalk.interfaces.controller.user;

import com.cc.booktalk.application.user.service.square.SquareFeedService;
import com.cc.booktalk.common.result.PageResult;
import com.cc.booktalk.common.result.Result;
import com.cc.booktalk.interfaces.dto.user.square.SquareFeedQueryDTO;
import com.cc.booktalk.interfaces.vo.user.square.SquareFeedVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/user/square")
@Api(tags = "广场相关接口")
public class SquareController {

    @Resource
    private SquareFeedService squareFeedService;

    @GetMapping("/feed")
    @ApiOperation("广场内容流")
    public Result<PageResult<SquareFeedVO>> getFeed(SquareFeedQueryDTO queryDTO) {
        return Result.success(squareFeedService.getFeed(queryDTO));
    }
}
