package com.cc.talkadmin.controller;


import com.cc.talkcommon.result.Result;
import com.cc.talkpojo.Result.PageResult;
import com.cc.talkpojo.dto.BookDTO;
import com.cc.talkpojo.dto.PageBookDTO;
import com.cc.talkpojo.vo.BookVO;
import com.cc.talkserver.admin.service.BookAdminService;
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
 * @since 2025-07-05
 */
@RestController
@RequestMapping("/admin/book")
@Api(tags = "图书管理接口")
@Slf4j
public class BookAdminController {

    @Resource
    private BookAdminService bookService;


    /**
     * 单本图书上传
     * @param bookDTO
     * @return
     */
    @ApiOperation("单本图书上传")
    @PostMapping("/upload")
    public Result<Object> bookUpload(@RequestBody BookDTO bookDTO) {
        log.info("单本图书上传,BookTitle:{}", bookDTO.getTitle());
        bookService.bookUpload(bookDTO);
        return Result.success("图书上传成功!");
    }

    /**
     * 图书批量上传
     * @param bookList
     * @return
     */
    @ApiOperation("批量图书上传")
    @PostMapping("/uploadBatch")
    public Result<Object> booksBatchUpload(@RequestBody List<BookDTO> bookList) {
        log.info("图书批量上传...");
        bookService.booksBatchUpload(bookList);
        return Result.success("图书批量上传成功!");
    }

    /**
     * 图书信息分页查询
     * @param pageDTO
     * @return
     */
    @ApiOperation("图书信息分页查询")
    @PostMapping("/page")
    public Result<PageResult<BookVO>> pageBook(@RequestBody PageBookDTO pageDTO) {
        log.info("图书信息分页查询....");
        PageResult<BookVO> pageResult = bookService.getBookPage(pageDTO);
        return Result.success(pageResult);
    }


    /**
     * 查询图书详信息
     * @param id
     * @return
     */
    @ApiOperation("图书详细信息查询")
    @GetMapping("/detail/{id}")
    public Result<BookVO> bookDetail(@PathVariable("id") Long id) {

        BookVO bookVO = bookService.getBookDetail(id);
        return Result.success(bookVO);
    }


    /**
     * 图书资料编辑
     * @param id
     * @param bookDTO
     * @return
     */
    @ApiOperation("图书资料编辑")
    @PutMapping("/update/{id}")
    public Result<Object> bookDetailRevise(@PathVariable("id") Long id,
                                           @RequestBody BookDTO bookDTO) {

        bookService.updateBookDetail(id,bookDTO);
        return Result.success();
    }


    /**
     * 单本图书删除
     * @param id
     * @return
     */
    @ApiOperation("单本图书删除")
    @DeleteMapping("/delete/{id}")
    public Result<Object> bookDelete(@PathVariable("id") Long id) {
        log.info("删除图书,id:{}",id);
        bookService.deleteById(id);
        return Result.success();
    }


    /**
     * 批量图书删除
     * @param ids
     * @return
     */
    @ApiOperation("图书批量删除")
    @DeleteMapping("/deleteBatch")
    public Result<Object> bookDeleteBatch(@RequestBody List<Integer> ids) {
        log.info("图书批量删除...");
        bookService.deleteByIdS(ids);
        return Result.success();
    }

}

