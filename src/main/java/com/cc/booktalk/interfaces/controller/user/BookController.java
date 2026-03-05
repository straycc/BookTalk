package com.cc.booktalk.interfaces.controller.user;


import com.cc.booktalk.common.result.Result;
import com.cc.booktalk.common.result.PageResult;
import com.cc.booktalk.infrastructure.aop.annotation.TrackUserBehavior;
import com.cc.booktalk.interfaces.dto.user.book.BookShowDTO;
import com.cc.booktalk.interfaces.dto.user.book.BookPageDTO;
import com.cc.booktalk.interfaces.dto.user.search.PageSearchDTO;
import com.cc.booktalk.interfaces.vo.user.book.BookVO;
import com.cc.booktalk.interfaces.vo.user.category.CategoryVO;
import com.cc.booktalk.application.user.service.book.BookUserService;

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
     * @param bookPageDTO
     * @return
     */
    @ApiOperation("书籍分页查询")
    @PostMapping("/page")
    public Result<PageResult<BookShowDTO>> page(@RequestBody BookPageDTO bookPageDTO) {
        PageResult<BookShowDTO> pageResult = bookUserService.getBookPage(bookPageDTO);
        return Result.success(pageResult);
    }

    /**
     * 获取书籍详情
     * @param id
     * @return
     */
    @ApiOperation("查询书籍详情")
    @GetMapping("/detail/{id}")
    @TrackUserBehavior(
            behaviorType = "BOOK_VIEW",
            targetType = "BOOK",
            targetIdParam = "id",
            behaviorScore = 1.0
    )
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
    public Result<PageResult<BookShowDTO>> PageByTag(@PathVariable("tagId") Long id, @RequestBody BookPageDTO bookPageDTO) {
         PageResult<BookShowDTO> pageResult = bookUserService.getPageByTag(id, bookPageDTO);
         return Result.success(pageResult);
    }
}
