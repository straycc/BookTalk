package com.cc.talkuser.controller;


import com.cc.talkcommon.result.Result;
import com.cc.talkpojo.dto.BookListDTO;
import com.cc.talkpojo.dto.BookListPageDTO;
import com.cc.talkpojo.result.PageResult;
import com.cc.talkpojo.vo.BookListDetailVO;
import com.cc.talkpojo.vo.BookListVO;
import com.cc.talkserver.user.service.BookListService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * <p>
 * 用户书单表 前端控制器
 * </p>
 *
 * @author cc
 * @since 2025-09-17
 */
@RestController
@RequestMapping("/user/book-list")
@Api(tags = "书单相关接口")
public class BookListController {


    @Resource
    private BookListService bookListService;


    /**
     * 新建书单
     * @param bookListDTO
     * @return
     */
    @ApiOperation("新建书单")
    @PostMapping("/newBookList")
    public Result<Object> newBookList(@RequestBody BookListDTO bookListDTO) {
        bookListService.createBookList(bookListDTO);
        return Result.success();
    }

    /**
     * 修改书单（标题/描述）
     */
    @ApiOperation("修改书单")
    @PutMapping("/update/{bookListId}")
    public Result<Object> updateBookList(@PathVariable Long bookListId,@RequestBody BookListDTO bookListDTO) {
        bookListService.updateBookList(bookListId,bookListDTO);
        return Result.success();
    }

    /**
     * 删除书单
     * @param bookListId
     * @return
     */
    @ApiOperation("删除书单")
    @DeleteMapping("/delete/bookListId")
    public Result<Object> deleteBookList(@PathVariable Long bookListId) {
        bookListService.deleteBookList(bookListId);
        return Result.success();
    }

    /**
     * 查询个人书单
     */
    @ApiOperation("我的书单")
    @PostMapping("/myList")
    public Result<Object> myList(@RequestBody BookListPageDTO bookListPageDTO) {
        PageResult<BookListVO> bookList  = bookListService.myBookListPage(bookListPageDTO);
        return Result.success(bookList);
    }

    /**
     * 查询书单详情
     */
    @ApiOperation("查询书单详情")
    @GetMapping("/detail")
    public Result<BookListDetailVO> detailBookList(@RequestParam("bookListId") Long bookListId) {
        BookListDetailVO bookListDetailVO = bookListService.getBookListDetail(bookListId);
        return Result.success(bookListDetailVO);
    }

    /**
     * 书单增加新书籍
     */
    @ApiOperation("书单增加新的书籍")
    @PostMapping("/addBook/{bookListId}")
    public Result<Object> addOne(@PathVariable Long bookListId ,@RequestBody BookListDTO bookListDTO) {
        bookListService.addBook(bookListId,bookListDTO);
        return Result.success();
    }

    /**
     * 书单删除书籍
     */
    @ApiOperation("书单删除书籍")
    @DeleteMapping("/deleteOne/{bookListId}")
    public Result<Object> deleteOne(@PathVariable Long bookListId, @RequestBody BookListDTO bookListDTO) {
        bookListService.deleteBook(bookListId,bookListDTO);
        return Result.success();
    }







}
