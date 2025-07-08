package com.cc.talkadmin.controller;


import com.cc.talkadmin.service.ICategoryService;
import com.cc.talkcommon.result.Result;
import com.cc.talkpojo.Result.CategoryUpResult;
import com.cc.talkpojo.Result.PageResult;
import com.cc.talkpojo.dto.CategoryDTO;
import com.cc.talkpojo.dto.PageCategoryDTO;
import com.cc.talkpojo.entity.BookCategory;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 图书分类表 前端控制器
 * </p>
 *
 * @author cc
 * @since 2025-07-07
 */
@RestController
@RequestMapping("book/category")
@Api(tags = "分类管理接口")
@Slf4j
public class CategoryController {


    @Resource
    private ICategoryService categoryService;


    /**
     * 分类分页查询
     * @return
     */
    @PostMapping("/page")
    @ApiOperation("获取分类列表")
    public Result<PageResult> getCategoryPage(@RequestBody PageCategoryDTO  pageCategoryDTO) {

        PageResult pageResult = categoryService.getCategoryPage(pageCategoryDTO);
        return Result.success(pageResult);
    }


    /**
     * 新增单个分类
     * @param categoryDTO
     * @return
     */
    @PostMapping("/signalAdd")
    @ApiOperation("新增单个分类")
    public Result<Object> addCategory(@RequestBody CategoryDTO categoryDTO){
        log.info("新增单个分类,name:{}",categoryDTO.getName());
        categoryService.signalAdd(categoryDTO);
        return Result.success("新增单个分类成功!");
    }


    /**
     * 批量新增分类
     * @param categoryList
     * @return
     */
    @PostMapping("/batchAdd")
    @ApiOperation("批量插入分类")
    public Result<Object> batchAddCategory(@RequestBody List<CategoryDTO> categoryList){
        log.info("批量新增分类...");
        CategoryUpResult categoryUpResult = categoryService.batchAddCategory(categoryList);
        return Result.success("批量新增分类成功!",categoryUpResult);
    }


    /**
     * 查询分类详细信
     * @param id
     * @return
     */
    @DeleteMapping("/detail/{id}")
    @ApiOperation("获取分类详细信息")
    public Result<CategoryDTO> getDetail(@PathVariable Long id){
        CategoryDTO categoryDTO = categoryService.getDetail(id);
        return Result.success(categoryDTO);
    }

    /**
     * 修改单个分类
     * @param categoryDTO
     * @return
     */
    @PutMapping("/revise")
    @ApiOperation("修改单个分类")
    public Result<Object> reviseCategory(@RequestBody CategoryDTO categoryDTO){
        log.info("修改分类,id:{}",categoryDTO.getId());
        categoryService.reviseCategory(categoryDTO);
        return Result.success();
    }

    /**
     * 单个分类删除
     * @param id
     * @return
     */
    @DeleteMapping("/signalDelete/{id}")
    @ApiOperation("单个分类删除")
    public Result<Object> deleteCategory(@PathVariable Long id){
        log.info("删除分类，id:{}",id);
        categoryService.deleteCategory(id);
        return Result.success();
    }


    /**
     * 批量删除分类
     * @param ids
     * @return
     */
    @DeleteMapping("/batchDelete")
    @ApiOperation("批量删除分类")
    public Result<Object> batchDeleteCategory(@RequestBody List<Long> ids){

        categoryService.batchDeleteCategory(ids);
        return Result.success();
    }

}
