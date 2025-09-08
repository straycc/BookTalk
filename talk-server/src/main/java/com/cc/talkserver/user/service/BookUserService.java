package com.cc.talkserver.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cc.talkpojo.Result.PageResult;
import com.cc.talkpojo.dto.BookShowDTO;
import com.cc.talkpojo.dto.PageBookDTO;
import com.cc.talkpojo.dto.PageSearchDTO;
import com.cc.talkpojo.entity.Book;
import com.cc.talkpojo.vo.BookVO;
import com.cc.talkpojo.vo.CategoryVO;
import com.cc.talkpojo.vo.TagVO;

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
     * @param pageBookDTO
     * @return
     */
    PageResult<BookShowDTO> getBookPage(PageBookDTO pageBookDTO);

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
    PageResult<BookShowDTO> getPageByTag(Long id,PageBookDTO pageBookDTO);


    /**
     * 查询缓存热门数据
     * @param pageBookDTO
     * @return
     */
    PageResult<BookShowDTO> getHotBook(PageBookDTO pageBookDTO);
}
