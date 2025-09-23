package com.cc.talkserver.admin.service;

import com.cc.talkpojo.result.CategoryUpResult;
import com.cc.talkpojo.result.PageResult;
import com.cc.talkpojo.dto.CategoryDTO;
import com.cc.talkpojo.dto.CategoryPageDTO;
import com.cc.talkpojo.entity.BookCategory;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * 图书分类表 服务类
 * </p>
 *
 * @author cc
 * @since 2025-07-14
 */
public interface CategoryAdminService extends IService<BookCategory> {


    /**
     * 分类分页查询
     * @param categoryPageDTO
     * @return
     */
    PageResult getCategoryPage(CategoryPageDTO categoryPageDTO);

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
