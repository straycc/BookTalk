package com.cc.booktalk.infrastructure.persistence.user.mapper.category;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cc.booktalk.entity.entity.category.Category;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 图书分类表 Mapper 接口
 * </p>
 *
 * @author cc
 * @since 2025-07-12
 */
@Mapper
public interface CategoryUserMapper extends BaseMapper<Category> {


    /**
     * 根据分类ids批量获取，<id,name>
     * @param categoryIds
     * @return
     */
    Map<Long, String> getCategoryNames(@Param("categoryIds") List<Long> categoryIds);
}
