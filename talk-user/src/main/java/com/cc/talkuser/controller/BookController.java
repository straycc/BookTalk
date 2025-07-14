package com.cc.talkuser.controller;


import com.cc.talkcommon.result.Result;
import com.cc.talkpojo.Result.PageResult;
import com.cc.talkpojo.dto.BookShowDTO;
import com.cc.talkpojo.dto.PageBookDTO;
import com.cc.talkpojo.vo.BookVO;
import com.cc.talkserver.user.service.BookUserService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * <p>
 * 图书主表 前端控制器
 * </p>
 *
 * @author cc
 * @since 2025-06-30
 */
@RestController
@RequestMapping("/book")
@Api("书籍相关接口")
public class BookController {


    @Resource
    private BookUserService bookUserService;



    /**
     * 书籍分页查询
     * @param pageBookDTO
     * @return
     */
    @ApiOperation("书籍分页查询")
    @PostMapping("/page")
    public Result<PageResult<BookShowDTO>> page(@RequestBody PageBookDTO pageBookDTO) {
        PageResult<BookShowDTO> pageResult = bookUserService.getBookPage(pageBookDTO);
        return Result.success(pageResult);
    }


    /**
     * 获取书籍详情
     * @param id
     * @return
     */
    @ApiOperation("查询书籍详情")
    @GetMapping("/detail")
    public Result<BookVO> detail(@RequestParam("id") Integer id) {

        BookVO bookVo = bookUserService.getBookDetail(id);
        return Result.success(bookVo);
    }


//    /**
//     * 获取所有分类信息
//     * @return
//     */
//    @ApiOperation("查询所有分类")
//    @GetMapping("/category/list")
//    public Result<List<CategoryVO>> listCategory() {
//        List<CategoryVO> categoryList = bookUserService.getCategoryList();
//        return Result.success(categoryList);
//    }
//
//    /**
//     * 获取所有标签信息
//     * @return
//     */
//    @ApiOperation("获取所有标签信息")
//    @GetMapping("/tag/list")
//    public Result<List<TagVO>> listTag() {
//        List<TagVO> tagVOList = bookUserService.getTagList();
//        return Result.success(tagVOList);
//    }
//
//
//    /**
//     * 根据标签分页查询书籍
//     * @param id
//     * @return
//     */
//    @ApiOperation("根据标签分页查询")
//    @GetMapping("/tag/{id}")
//    public Result<PageResult<BookVO>> PageByTag(@PathVariable("id") Integer id) {
//         PageResult<BookVO> pageResult = bookUserService.getPageByTag(id);
//         return Result.success(pageResult);
//    }
//
//
//    @ApiOperation("查询热门标签")
//    @GetMapping("/tag/getHot/{categoryId}")
//    public Result<List<BookVO>> getHot(@PathVariable Long categoryId) {
//
//        List<TagVO> tagVOList = bookUserService.getHotTags(categoryId);
//        return Result.success();
//    }
//
//















}
