package com.cc.booktalk.infrastructure.persistence.user.mapper.bookShelf;

import com.cc.booktalk.domain.entity.bookShelf.BookShelf;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cc.booktalk.interfaces.vo.user.bookShelf.BookShelfVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author cc
 * @since 2025-10-12
 */
public interface BookShelfMapper extends BaseMapper<BookShelf> {

    List<BookShelfVO> selectShelfPage(@Param("userId") Long userId,
                                      @Param("status") String status,
                                      @Param("bookName") String bookName,
                                      @Param("sortBy") String sortBy,
                                      @Param("sortOrder") String sortOrder);
}
