package com.cc.booktalk.application.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cc.booktalk.common.exception.BaseException;
import com.cc.booktalk.common.utils.ConvertUtils;
import com.cc.booktalk.common.result.CategoryUpResult;
import com.cc.booktalk.common.result.PageResult;
import com.cc.booktalk.interfaces.dto.user.category.CategoryDTO;
import com.cc.booktalk.interfaces.dto.user.category.CategoryPageDTO;
import com.cc.booktalk.domain.entity.book.Book;
import com.cc.booktalk.domain.entity.category.Category;
import com.cc.booktalk.infrastructure.persistence.admin.mapper.BookAdminMapper;
import com.cc.booktalk.infrastructure.persistence.admin.mapper.CategoryAdminMapper;
import com.cc.booktalk.application.admin.service.CategoryAdminService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageHelper;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 图书分类表 服务实现类
 * </p>
 *
 * @author cc
 * @since 2025-07-14
 */
@Service
public class CategoryAdminServiceImpl extends ServiceImpl<CategoryAdminMapper, Category> implements CategoryAdminService {
    @Resource
    private CategoryAdminMapper categoryAdminMapper;


    @Resource
    private BookAdminMapper bookAdminMapper;


    /**
     * 分类分页查询
     * @param categoryPageDTO
     * @return
     */
    @Override
    public PageResult getCategoryPage(CategoryPageDTO categoryPageDTO) {
        if(categoryPageDTO ==null || categoryPageDTO.getPageNum() < 1 || categoryPageDTO.getPageSize() < 1){
            throw new BaseException("分页参数有误!");
        }

        //1. 开始分页
        PageHelper.startPage(categoryPageDTO.getPageNum(), categoryPageDTO.getPageSize());

        //2. 分页查询条件
        LambdaQueryWrapper<Category> queryWrapper = new LambdaQueryWrapper<>();
        if(categoryPageDTO.getName()!= null && !categoryPageDTO.getName().trim().isEmpty()){
            queryWrapper.like(Category::getName, categoryPageDTO.getName());
        }

        //3. 执行分页查询
        List<Category> list = categoryAdminMapper.selectList(queryWrapper);

        //4. 转换对象
        List<CategoryDTO> categoryList = list.stream().map(
                category -> ConvertUtils.convert(category,CategoryDTO.class)
        ) .collect(Collectors.toList());


        return new PageResult<>(categoryList.size(),categoryList);
    }

    /**
     * 新增单个分类
     * @param categoryDTO
     */
    @Override
    public void signalAdd(CategoryDTO categoryDTO) {
        if(categoryDTO == null || categoryDTO.getName() == null || categoryDTO.getName().isEmpty()){
            throw new BaseException("参数不能为空!");
        }
        //1. 检查分类是否存在
        boolean exists = lambdaQuery().eq(Category::getName, categoryDTO.getName()).exists();
        if(exists){
            throw new BaseException("分类已存在!");
        }

        //2. 存入数据库
        Category category = new Category();
        BeanUtils.copyProperties(categoryDTO, category);
        int num = categoryAdminMapper.insert(category);
        if(num != 1){
            throw new BaseException("新增分类失败!");
        }
    }

    /**
     * 批量新增分类
     * @param categoryList
     */
    @Override
    public CategoryUpResult batchAddCategory(List<CategoryDTO> categoryList) {
        if(categoryList == null || categoryList.isEmpty()){
            throw new BaseException("分类列表参数为空!");
        }

        ArrayList<String> existCategoryList = new ArrayList<>();
        ArrayList<String> emptyCategoryList = new ArrayList<>();
        ArrayList<Category> bookCategoryList = new ArrayList<>();

        for(CategoryDTO categoryDTO : categoryList){
            String name = categoryDTO.getName();
            //分类名为空
            if(name == null || name.isEmpty()){
                emptyCategoryList.add("分类描述:"+categoryDTO.getDescription());
                continue;
            }
            //分类已存在
            if(lambdaQuery().eq(Category::getName, name).exists()){
                existCategoryList.add(name);
                continue;
            }
            //带插入分类
            Category category = new Category();
            BeanUtils.copyProperties(categoryDTO, category);
            category.setCreateTime(LocalDateTime.now());
            category.setUpdateTime(LocalDateTime.now());
            bookCategoryList.add(category);
        }
        if(categoryList.isEmpty()){
            throw new BaseException("批量插入分类失败!");
        }

        // 存入数据库
        saveBatch(bookCategoryList);

        return new CategoryUpResult(
                categoryList.size(),
                emptyCategoryList,
                existCategoryList
        );
    }


    /**
     * 查询分类详情
     * @param id
     * @return
     */
    @Override
    public CategoryDTO getDetail(Long id) {
        if(!verifyParam(id)){
            throw new BaseException("分类id信息有误!");
        }
        Category category = categoryAdminMapper.selectById(id);
        if(category == null){
            throw new BaseException("当前分类不存在");
        }
        CategoryDTO categoryDTO = new CategoryDTO();
        BeanUtils.copyProperties(category,categoryDTO);
        return categoryDTO;
    }


    /**
     * 修改单个分类
     * @param categoryDTO
     */
    @Override
    public void reviseCategory(CategoryDTO categoryDTO) {
        if(!verifyParam(categoryDTO.getId())){
            throw new BaseException("分类id信息有误!");
        }
        Category category = new Category();
        BeanUtils.copyProperties(categoryDTO, category);
        category.setUpdateTime(LocalDateTime.now());
        categoryAdminMapper.updateById(category);
    }


    /**
     *删除单个分类
     * @param id
     */
    @Override
    public void deleteCategory(Long id) {
        if(!verifyParam(id)){
            throw new BaseException("分类id信息有误!");
        }

        int num = categoryAdminMapper.deleteById(id);
        if(num != 1){
            throw new BaseException("删除分类失败!");
        }
    }


    /**
     * 分类批量删除
     * @param ids
     */
    @Override
    public void batchDeleteCategory(List<Long> ids) {
        if(ids == null || ids.isEmpty()){
            throw new BaseException("参数为空!");
        }


        Long count = bookAdminMapper.selectCount(new QueryWrapper<Book>().in("category_id", ids));
        if (count > 0) {
            throw new BaseException("部分分类已被使用，无法删除！");
        }
        int num = categoryAdminMapper.deleteByIds(ids);
        if(num != ids.size()){
            throw new BaseException("批量删除分类失败!");
        }

    }


    /**
     * 检验参数（分类id）
     * @param id
     * @return
     */
    public boolean verifyParam(Long id){
        if(id == null || id <= 0){
            return false;
        }
        return true;
    }
}
