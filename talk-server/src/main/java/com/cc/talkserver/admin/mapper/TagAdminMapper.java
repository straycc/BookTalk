package com.cc.talkserver.admin.mapper;

import com.cc.talkpojo.entity.Tag;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 标签表 Mapper 接口
 * </p>
 *
 * @author cc
 * @since 2025-07-14
 */
@Mapper
public interface TagAdminMapper extends BaseMapper<Tag> {

}
