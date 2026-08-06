package com.cc.booktalk.interfaces.controller.user;

import com.cc.booktalk.application.user.service.post.PostService;
import com.cc.booktalk.common.result.PageResult;
import com.cc.booktalk.common.result.Result;
import com.cc.booktalk.infrastructure.aop.annotation.TrackUserBehavior;
import com.cc.booktalk.interfaces.dto.user.post.PostCreateDTO;
import com.cc.booktalk.interfaces.dto.user.post.PostPageDTO;
import com.cc.booktalk.interfaces.vo.user.post.PostVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@RestController
@RequestMapping("/user/posts")
@Api(tags = "帖子相关接口")
public class PostController {

    @Resource
    private PostService postService;

    @PostMapping
    @ApiOperation("发帖")
    public Result<Long> createPost(@RequestBody PostCreateDTO postCreateDTO) {
        return Result.success(postService.createPost(postCreateDTO));
    }

    @DeleteMapping("/{postId}")
    @ApiOperation("删除帖子")
    public Result<Void> deletePost(@PathVariable Long postId) {
        postService.deletePost(postId);
        return Result.success();
    }

    @GetMapping("/{postId}")
    @ApiOperation("帖子详情")
    @TrackUserBehavior(behaviorType = "POST_VIEW", targetType = "POST", targetIdParam = "postId", behaviorScore = 0.2)
    public Result<PostVO> getPostDetail(@PathVariable Long postId) {
        return Result.success(postService.getPostDetail(postId));
    }

    @GetMapping("/page")
    @ApiOperation("帖子分页")
    public Result<PageResult<PostVO>> getPostPage(PostPageDTO postPageDTO) {
        return Result.success(postService.getPostPage(postPageDTO));
    }
}
