package com.cc.talkserver.user.service;

import com.cc.talkpojo.dto.BookShelfAddDTO;
import com.cc.talkpojo.dto.BookShelfQueryDTO;
import com.cc.talkpojo.entity.BookShelf;
import com.cc.talkpojo.result.PageResult;
import com.cc.talkpojo.vo.BookShelfVO;
import com.cc.talkpojo.vo.BookShelfStatsVO;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 个人书架服务接口
 *
 * @author cc
 * @since 2025-10-12
 */
public interface BookShelfService extends IService<BookShelf> {

    /**
     * 添加书籍到书架
     * @param addDTO 添加书籍DTO
     */
    void addToShelf(BookShelfAddDTO addDTO);

    /**
     * 从书架移除书籍
     * @param shelfId 书架项ID
     */
    void removeFromShelf(Long shelfId);

    /**
     * 更新书籍状态
     * @param shelfId 书架项ID
     * @param status 阅读状态
     */
    void updateStatus(Long shelfId, String status);

    /**
     * 获取书架列表
     * @param queryDTO 查询条件
     * @return 书架列表
     */
    PageResult<BookShelfVO> getShelfList(BookShelfQueryDTO queryDTO);

    /**
     * 获取书架统计信息
     * @return 统计信息
     */
    BookShelfStatsVO getShelfStats();

    /**
     * 检查书籍是否在书架中
     * @param bookId 书籍ID
     * @return 是否在书架中
     */
    Boolean checkBookInShelf(Long bookId);
}
