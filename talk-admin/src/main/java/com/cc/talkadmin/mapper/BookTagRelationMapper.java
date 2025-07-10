package com.cc.talkadmin.mapper;

import com.cc.talkpojo.entity.BookTagRelation;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 图书标签表 Mapper 接口
 * </p>
 *
 * @author cc
 * @since 2025-07-09
 */
public interface BookTagRelationMapper extends BaseMapper<BookTagRelation> {

    List<Long> selectUsedTagIds(@Param("idList") List<Long> idList);
}
