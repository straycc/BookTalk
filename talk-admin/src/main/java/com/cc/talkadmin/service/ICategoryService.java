package com.cc.talkadmin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cc.talkpojo.Result.CategoryUpResult;
import com.cc.talkpojo.Result.PageResult;
import com.cc.talkpojo.dto.CategoryDTO;
import com.cc.talkpojo.dto.PageCategoryDTO;
import com.cc.talkpojo.entity.BookCategory;

import java.util.List;

public interface ICategoryService extends IService<BookCategory> {


    /**
     * 分类分页查询
     * @param pageCategoryDTO
     * @return
     */
    PageResult getCategoryPage(PageCategoryDTO pageCategoryDTO);

    /**
     * 新增单个分类
     * @param categoryDTO
     */
    void signalAdd(CategoryDTO categoryDTO);

    /**
     * 批量新增分类
     * @param categoryList
     */
    CategoryUpResult batchAddCategory(List<CategoryDTO> categoryList);


    /**
     * 查询分类详情
     * @param id
     * @return
     */
    CategoryDTO getDetail(Long id);

    /**
     * 修改单个分类
     * @param categoryDTO
     */
    void reviseCategory(CategoryDTO categoryDTO);


    /**
     * 删除单个分类
     * @param id
     */
    void deleteCategory(Long id);

    /**
     * 分类批量删除
     * @param ids
     */
    void batchDeleteCategory(List<Long> ids);


}
