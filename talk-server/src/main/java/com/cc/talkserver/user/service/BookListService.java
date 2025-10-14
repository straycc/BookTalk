package com.cc.talkserver.user.service;

import com.cc.talkpojo.dto.BookListDTO;
import com.cc.talkpojo.dto.BookListPageDTO;
import com.cc.talkpojo.entity.BookList;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cc.talkpojo.result.PageResult;
import com.cc.talkpojo.vo.BookListDetailVO;
import com.cc.talkpojo.vo.BookListVO;

/**
 * <p>
 * 用户书单表 服务类
 * </p>
 *
 * @author cc
 * @since 2025-09-17
 */
public interface BookListService extends IService<BookList> {


    /**
     * 新建书单
     * @param bookListDTO
     */
    void createBookList(BookListDTO bookListDTO);


    /**
     * 修改书单
     * @param bookListDTO
     */
    void updateBookList(Long bookListId,BookListDTO bookListDTO);


    /**
     * 删除书单
     * @param bookListId
     */
    void deleteBookList(Long bookListId);


    /**
     * 我的书单
     * @param bookListPageDTO
     * @return
     */
    PageResult<BookListVO> myBookListPage(BookListPageDTO bookListPageDTO);


    /**
     * 查询书单详情
     * @param bookListId
     * @return
     */
    BookListDetailVO getBookListDetail(Long bookListId);


    /**
     * 书单新增书籍
     * @param bookListId
     * @param bookListDTO
     */
    void addBook(Long bookListId, BookListDTO bookListDTO);


    /**
     * 书单删除书籍
     * @param bookListId
     * @param bookListDTO
     *
     */
    void deleteBook(Long bookListId, BookListDTO bookListDTO);
}
