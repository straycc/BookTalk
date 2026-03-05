package com.cc.booktalk.infrastructure.persistence.user.mapper.tag;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cc.booktalk.domain.entity.tag.BookTagRelation;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * <p>
 * 图书标签表 Mapper 接口
 * </p>
 *
 * @author cc
 * @since 2025-07-09
 */

@Mapper
public interface BookTagUserMapper extends BaseMapper<BookTagRelation> {


    List<Long> selectTagIdsByBookId(Long bookId);
}
