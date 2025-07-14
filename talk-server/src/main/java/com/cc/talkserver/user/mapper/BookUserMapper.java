package com.cc.talkserver.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cc.talkpojo.entity.Book;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 图书主表 Mapper 接口
 * </p>
 *
 * @author cc
 * @since 2025-06-30
 */
@Mapper
public interface BookUserMapper extends BaseMapper<Book> {

}
