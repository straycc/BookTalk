package com.cc.talkserver.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cc.talkpojo.Result.PageResult;
import com.cc.talkpojo.dto.BookShowDTO;
import com.cc.talkpojo.dto.PageBookDTO;
import com.cc.talkpojo.entity.Book;
import com.cc.talkpojo.vo.BookVO;

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
     * 书籍分页查询
     * @param pageBookDTO
     * @return
     */
    PageResult<BookShowDTO> getBookPage(PageBookDTO pageBookDTO);


    /**
     * 获取书籍详情
     * @param id
     * @return
     */
    BookVO getBookDetail(Integer id);
}
