package com.cc.talkuser.controller;


import com.cc.talkcommon.result.Result;
import com.cc.talkpojo.Result.PageResult;
import com.cc.talkpojo.dto.BookShowDTO;
import com.cc.talkpojo.dto.PageBookDTO;
import com.cc.talkpojo.dto.PageSearchDTO;
import com.cc.talkpojo.vo.BookVO;
import com.cc.talkpojo.vo.CategoryVO;
import com.cc.talkserver.user.service.BookUserService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * <p>
 * 图书主表 前端控制器
 * </p>
 *
 * @author cc
 * @since 2025-06-30
 */
@RestController
@RequestMapping("user/book")
@Api(tags = "书籍相关接口")
@Slf4j
public class BookController {


    @Resource
    private BookUserService bookUserService;

    /**
     * 书籍全局搜索
     * @param pageSearchDTO
     * @return
     */
    @ApiOperation("书籍全局搜索")
    @PostMapping("/search")
    public Result<PageResult<BookShowDTO>> bookSearch(@RequestBody PageSearchDTO pageSearchDTO) {

        PageResult<BookShowDTO> pageResult = bookUserService.getSearchPage(pageSearchDTO);
        return Result.success(pageResult);
    }


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
    @GetMapping("/detail/{id}")
    public Result<BookVO> detail(@PathVariable Long id) {
        log.info("查询bookId:{},书籍详情",id);
        BookVO bookVo = bookUserService.getBookDetail(id);
        return Result.success(bookVo);
    }


    /**
     * 获取所有分类信息
     * @return
     */
    @ApiOperation("查询所有分类")
    @GetMapping("/category/list")
    public Result<List<CategoryVO>> listCategory() {
        List<CategoryVO> categoryList = bookUserService.getCategoryList();
        return Result.success(categoryList);
    }



    /**
     * 根据标签分页查询书籍
     * @param id
     * @return
     */
    @ApiOperation("根据标签查询书籍")
    @PostMapping("/tag/{tagId}")
    public Result<PageResult<BookShowDTO>> PageByTag(@PathVariable("tagId") Long id, @RequestBody PageBookDTO pageBookDTO) {
         PageResult<BookShowDTO> pageResult = bookUserService.getPageByTag(id,pageBookDTO);
         return Result.success(pageResult);
    }


    /**
     * 热门书籍分页查询
     * @param pageBookDTO
     * @return
     */
    @ApiOperation("热门书籍查询")
    @GetMapping("/hootBook")
    public Result<PageResult<BookShowDTO>> hootBook(@RequestBody PageBookDTO pageBookDTO) {

        PageResult <BookShowDTO> hotBookPage = bookUserService.getHotBook(pageBookDTO);
        return Result.success(hotBookPage);
    }



}
