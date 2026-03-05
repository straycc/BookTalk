package com.cc.booktalk.infrastructure.persistence.admin.mapper;

import com.cc.booktalk.domain.entity.category.Category;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 图书分类表 Mapper 接口
 * </p>
 *
 * @author cc
 * @since 2025-07-14
 */
@Mapper
public interface CategoryAdminMapper extends BaseMapper<Category> {

}
