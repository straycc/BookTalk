package com.cc.booktalk.infrastructure.persistence.user.mapper.book;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cc.booktalk.domain.entity.book.Book;
import com.cc.booktalk.interfaces.vo.user.rec.PersonalizedRecVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

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

    /**
     * 批量查询推荐场景所需的图书基础信息
     *
     * @param bookIds 书籍ID列表
     * @return 推荐视图列表
     */
    List<PersonalizedRecVO> getRecBookBaseByIds(@Param("bookIds") List<Long> bookIds);

    /**
     * 无行为日志时的热门回退推荐
     *
     * @param limit 推荐数量
     * @return 回退热门列表
     */
    List<PersonalizedRecVO> getFallbackHotBooks(@Param("limit") Integer limit);
}
