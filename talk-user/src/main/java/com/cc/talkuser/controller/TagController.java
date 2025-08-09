package com.cc.talkuser.controller;


import com.cc.talkcommon.constant.BusinessConstant;
import com.cc.talkcommon.result.Result;
import com.cc.talkpojo.dto.TagDTO;
import com.cc.talkpojo.vo.TagVO;
import com.cc.talkpojo.vo.UserVO;
import com.cc.talkserver.user.service.TagUserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * <p>
 * 标签表 前端控制器
 * </p>
 *
 * @author cc
 * @since 2025-08-02
 */
@RestController
@RequestMapping("/tag")
@Slf4j
@Api(tags = "标签相关功能")

public class TagController {


    @Resource
    private TagUserService tagUserService;


    /**
     * 根据分类id获取标签列表
     * @param categoryId
     * @return
     */
    @ApiOperation("根据分类获取标签列表")
    @GetMapping("/category/{categoryId}")
    public Result<List<TagVO>> findByCategoryId(@PathVariable("categoryId") Long categoryId) {
        List<TagVO> tagVOList = tagUserService.findByCategoryId(categoryId);
        return Result.success(tagVOList);
    }

    /**
     * 查询标签详情
     * @param tagId
     * @return
     */
    @ApiOperation("查询标签详情")
    @GetMapping("/detail/{id}")
    public Result<TagVO> getTagDetail(@PathVariable("id") Long tagId) {
        TagVO tagVO = tagUserService.getTagDetail(tagId);
        return Result.success(tagVO);
    }

    /**
     * 用户新建标签
     * @param tagDTO
     * @return
     */
    @ApiOperation("用户新建标签")
    @PostMapping("/add")
    public Result<String> creatNewTag(@RequestBody TagDTO tagDTO) {
        tagUserService.creatNewTag(tagDTO);
        return Result.success(BusinessConstant.TAG_CREAT_SUCESS);
    }

    /**
     * 用户删除标签
     * @param tagId
     * @return
     */
    @ApiOperation("用户删除标签")
    @DeleteMapping("/delete/{id}")
    public Result<String> deleteTagById(@PathVariable("id") Long tagId) {
        tagUserService.deleteTagById(tagId);
        return Result.success(BusinessConstant.TAG_DELETE_SUCESS);
    }

    /**
     * 用户修改标签
     * @param tagId 标签ID
     * @param tagDTO 标签DTO（包含新名称、描述等）
     * @return
     */
    @ApiOperation("用户修改标签")
    @PutMapping("/update/{tagId}")
    public Result<String> updateTag(@PathVariable("tagId") Long tagId, @RequestBody TagDTO tagDTO) {
        tagUserService.updateTag(tagDTO);
        return Result.success(BusinessConstant.TAG_UPDATE_SUCESS);
    }

    /**
     * 查询用户所有标签（标签较少暂不使用分页）
     * @param userId
     * @return
     */
    @ApiOperation("查询用户所有标签")
    @GetMapping("/user/{userId}")
    public Result<List<TagVO>> getUserTags(@PathVariable("userId") Long userId) {
         List<TagVO> tagVOList = tagUserService.getUserTags(userId);
        return Result.success(tagVOList);
    }


    /**
     * 获取热度标签
     */
    @ApiOperation("获取热度标签")
    @GetMapping("/hotTag")
    public Result<List<TagVO>> hotTag() {
        return Result.success();
    }

}
