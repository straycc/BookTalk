package com.cc.booktalk.application.user.service.book;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cc.booktalk.common.result.PageResult;
import com.cc.booktalk.interfaces.dto.user.book.BookShowDTO;
import com.cc.booktalk.interfaces.dto.user.book.BookPageDTO;
import com.cc.booktalk.interfaces.dto.user.search.PageSearchDTO;
import com.cc.booktalk.domain.entity.book.Book;
import com.cc.booktalk.interfaces.vo.user.book.BookVO;
import com.cc.booktalk.interfaces.vo.user.category.CategoryVO;

import java.util.List;

/**
 * <p>
 * 图书主表 服务类
 * </p>
 *
 * @author cc
 * @since 2025-06-30
 */
public interface BookUserService extends IService<Book> {


    /**
     * 图书搜索（书名/作者/ISBN）
     * @param pageSearchDTO
     * @return
     */
    PageResult<BookShowDTO> getSearchPage(PageSearchDTO pageSearchDTO);

    /**
     * 书籍概览分页查询
     * @param bookPageDTO
     * @return
     */
    PageResult<BookShowDTO> getBookPage(BookPageDTO bookPageDTO);

    /**
     * 获取书籍详情
     * @param id
     * @return
     */
    BookVO getBookDetail(Long id);

    /**
     * 获取分类列表
     * @return
     */
    List<CategoryVO> getCategoryList();

    /**
     * 根据标签分页查询图书
     * @param id
     * @return
     */
    PageResult<BookShowDTO> getPageByTag(Long id, BookPageDTO bookPageDTO);

}
