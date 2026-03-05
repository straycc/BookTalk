package com.cc.booktalk.application.user.service.bookShelf;

import com.cc.booktalk.interfaces.dto.user.bookShelf.BookShelfAddDTO;
import com.cc.booktalk.interfaces.dto.user.bookShelf.BookShelfQueryDTO;
import com.cc.booktalk.domain.entity.bookShelf.BookShelf;
import com.cc.booktalk.common.result.PageResult;
import com.cc.booktalk.interfaces.vo.user.bookShelf.BookShelfVO;
import com.cc.booktalk.interfaces.vo.user.bookShelf.BookShelfStatsVO;
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
