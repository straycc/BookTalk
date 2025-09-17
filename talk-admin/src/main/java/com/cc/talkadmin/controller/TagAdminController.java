package com.cc.talkadmin.controller;


import com.cc.talkcommon.result.Result;
import com.cc.talkcommon.utils.ConvertUtils;
import com.cc.talkpojo.result.PageResult;
import com.cc.talkpojo.result.TagUpResult;
import com.cc.talkpojo.dto.PageTagDTO;
import com.cc.talkpojo.dto.TagDTO;
import com.cc.talkpojo.vo.TagVO;
import com.cc.talkserver.admin.service.TagAdminService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 图书标签表 前端控制器
 * </p>
 *
 * @author cc
 * @since 2025-07-09
 */
@RestController
@RequestMapping("/book/tag")
@Api(tags = "标签管理")
public class TagAdminController {

    @Resource
    private TagAdminService tagAdminService;


    /**
     * 标签分页查询
     * @param pageTagDTO
     * @return
     */
    @ApiOperation("标签分页查询")
    @PostMapping("/page")
    public Result<PageResult<TagVO>> page(@RequestBody PageTagDTO pageTagDTO) {
         PageResult<TagVO> pageResult = tagAdminService.getPage(pageTagDTO);
        return Result.success(pageResult);
    }


    /**
     * 新增单个标签
     * @param bookTagDTO
     * @return
     */
    @ApiOperation("新增单个标签")
    @PostMapping("/singleAdd")
    public Result<Void> tagSignalAdd(@RequestBody TagDTO bookTagDTO) {
        tagAdminService.tagSignalAdd(bookTagDTO);
        return Result.success();
    }


    /**
     * 批量新增标签
     * @param bookTagsList
     * @return
     */
    @ApiOperation("批量新增标签")
    @PostMapping("/batchAdd")
    public Result<TagUpResult> tagsBatchAdd(@RequestBody List<TagDTO> bookTagsList) {
        TagUpResult tagUpResult =  tagAdminService.tagsBatchAdd(bookTagsList);
        return Result.success(tagUpResult);
    }


    /**
     * 查询标签详情
     * @param id
     * @return
     */
    @ApiOperation("查询标签详情")
    @GetMapping("/getDetail/{id}")
    public Result<TagVO> getTagDetail(@PathVariable Long id) {
        TagVO tagVO = tagAdminService.getDetail(id);
        return Result.success(tagVO);
    }

    /**
     * 修改标签
     * @param id
     * @param bookTagDTO
     * @return
     */
    @ApiOperation("修改标签")
    @PostMapping("/tagRevise/{id}")
    public Result<Void> tagRevise(@PathVariable Long id, @RequestBody TagDTO bookTagDTO){
        tagAdminService.tagRevise(id,bookTagDTO);
        return Result.success();
    }


    /**
     * 删除单个标签
     * @param id
     * @return
     */
    @ApiOperation("删除单个标签")
    @DeleteMapping("/singleDelete/{id}")
    public Result<Void> tagSignalDelete(@PathVariable Long id) {
        tagAdminService.tagSignalDelete(id);
        return Result.success();
    }


    /**
     * 批量删除标签
     * @param idList
     * @return
     */
    @ApiOperation("批量删除标签")
    @DeleteMapping("/tagsBatchDelete")
    public Result<Void> tagsBatchDelete(@RequestBody List<Long> idList) {
        tagAdminService.tagsBatchDelete(idList);
        return Result.success();
    }

    /**
     * 获取所有标签列表
     * @return
     */
    @ApiOperation("获取所有标签列表")
    @GetMapping("/list")
    public Result<List<TagVO>> getAllTags() {
        List<TagVO> tagVOList = tagAdminService.list().stream().map(
                bookTag -> ConvertUtils.convert(bookTag, TagVO.class)
        ).collect(Collectors.toList());
        return Result.success(tagVOList);
    }


    /**
     * 根据分类 ID查询标签
     * @param categoryId
     * @return
     */
    @ApiOperation("根据分类 ID 查询标签")
    @GetMapping("/listByCategory/{categoryId}")
    public Result<List<TagVO>> listTagsByCategory(@PathVariable Long categoryId) {
        return Result.success(tagAdminService.getByCategoryId(categoryId));
    }


    /**
     * 检查标签是否存在
     * @param name
     * @return
     */
    @ApiOperation("校验标签名称是否存在")
    @GetMapping("/checkName")
    public Result<Boolean> checkTagExist(@RequestParam String name) {
        return Result.success(tagAdminService.checkTagExist(name));
    }



}
