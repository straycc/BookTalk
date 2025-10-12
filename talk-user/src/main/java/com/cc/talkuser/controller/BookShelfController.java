package com.cc.talkuser.controller;

import com.cc.talkcommon.constant.BusinessConstant;
import com.cc.talkcommon.result.Result;
import com.cc.talkpojo.dto.BookShelfAddDTO;
import com.cc.talkpojo.dto.BookShelfQueryDTO;
import com.cc.talkpojo.result.PageResult;
import com.cc.talkpojo.vo.BookShelfVO;
import com.cc.talkpojo.vo.BookShelfStatsVO;
import com.cc.talkserver.user.service.BookShelfService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;

/**
 * <p>
 * 个人书架 前端控制器
 * </p>
 *
 * @author cc
 * @since 2025-10-12
 */
@RestController
@RequestMapping("/user/book-shelf")
@Api(tags = "个人书架接口")
public class BookShelfController {

    @Resource
    private BookShelfService bookShelfService;

    /**
     * 添加书籍到书架
     * @param addDTO 添加书籍DTO
     * @return 操作结果
     */
    @ApiOperation("添加书籍到书架")
    @PostMapping("/add")
    public Result<Object> addToShelf(@RequestBody BookShelfAddDTO addDTO) {
        bookShelfService.addToShelf(addDTO);
        return Result.success(BusinessConstant.BOOK_SHELF_ADD_SUCCESS);
    }

    /**
     * 从书架移除书籍
     * @param shelfId 书架项ID
     * @return 操作结果
     */
    @ApiOperation("从书架移除书籍")
    @DeleteMapping("/remove/{shelfId}")
    public Result<Object> removeFromShelf(
            @ApiParam("书架项ID") @PathVariable Long shelfId) {
        bookShelfService.removeFromShelf(shelfId);
        return Result.success(BusinessConstant.BOOK_SHELF_REMOVE_SUCCESS);
    }

    /**
     * 更新书籍状态
     * @param shelfId 书架项ID
     * @param status 阅读状态
     * @return 操作结果
     */
    @ApiOperation("更新书籍状态")
    @PutMapping("/status/{shelfId}")
    public Result<Object> updateStatus(
            @ApiParam("书架项ID") @PathVariable Long shelfId,
            @ApiParam("阅读状态: WANT_TO_READ-想读, READING-在读, READ-读完") @RequestParam String status) {
        bookShelfService.updateStatus(shelfId, status);
        return Result.success(BusinessConstant.BOOK_SHELF_UPDATE_SUCCESS);
    }

    /**
     * 获取书架列表
     * @param queryDTO 查询条件
     * @return 书架列表
     */
    @ApiOperation("获取书架列表")
    @PostMapping("/list")
    public Result<PageResult<BookShelfVO>> getShelfList(@RequestBody BookShelfQueryDTO queryDTO) {
        PageResult<BookShelfVO> result = bookShelfService.getShelfList(queryDTO);
        return Result.success(result);
    }

    /**
     * 获取书架统计信息
     * @return 统计信息
     */
    @ApiOperation("获取书架统计信息")
    @GetMapping("/stats")
    public Result<BookShelfStatsVO> getShelfStats() {
        BookShelfStatsVO stats = bookShelfService.getShelfStats();
        return Result.success(stats);
    }

    /**
     * 检查书籍是否在书架中
     * @param bookId 书籍ID
     * @return 是否在书架中
     */
    @ApiOperation("检查书籍是否在书架中")
    @GetMapping("/check/{bookId}")
    public Result<Boolean> checkBookInShelf(
            @ApiParam("书籍ID") @PathVariable Long bookId) {
        Boolean inShelf = bookShelfService.checkBookInShelf(bookId);
        return Result.success(inShelf);
    }
}
