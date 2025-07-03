package com.cc.talkadmin.service;

import com.cc.talkpojo.Result.PageResult;
import com.cc.talkpojo.Result.UploadResult;
import com.cc.talkpojo.dto.BookDTO;
import com.cc.talkpojo.dto.PageBookDTO;
import com.cc.talkpojo.entity.Book;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cc.talkpojo.vo.BookVO;

import java.util.List;

/**
 * <p>
 * 图书主表 服务类
 * </p>
 *
 * @author cc
 * @since 2025-07-05
 */
public interface IBookService extends IService<Book> {

    /**
     * 单本图书上传
     * @param bookDTO
     */
    void bookUpload(BookDTO bookDTO);

    /**
     * 图书批量上传
     * @param bookList
     */
    UploadResult booksBatchUpload(List<BookDTO> bookList);

    /**
     * 图书信息分页查询
     * @param pageDTO
     * @return
     */
    PageResult<BookVO> getBookPage(PageBookDTO pageDTO);


    /**
     * 查询书记详细信息
     * @param id
     * @return
     */
    BookVO getBookDetail(Long id);

    /**
     * 图书信息编辑
     * @param id
     * @param bookDTO
     */
    void updateBookDetail(Long id, BookDTO bookDTO);


    /**
     * 单本图书删除
     * @param id
     */
    void deleteById(Long id);


    /**
     * 批量删除图书
     * @param ids
     */
    void deleteByIdS(List<Integer> ids);
}
