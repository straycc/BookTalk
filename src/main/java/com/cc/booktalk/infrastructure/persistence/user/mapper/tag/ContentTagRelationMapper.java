package com.cc.booktalk.infrastructure.persistence.user.mapper.tag;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cc.booktalk.domain.entity.tag.ContentTagRelation;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ContentTagRelationMapper extends BaseMapper<ContentTagRelation> {
}
