package com.cc.talkserver.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cc.talkcommon.constant.BusinessConstant;
import com.cc.talkcommon.context.UserContext;
import com.cc.talkcommon.exception.BaseException;
import com.cc.talkpojo.dto.BookShelfAddDTO;
import com.cc.talkpojo.dto.BookShelfQueryDTO;
import com.cc.talkpojo.entity.Book;
import com.cc.talkpojo.entity.BookShelf;
import com.cc.talkpojo.result.PageResult;
import com.cc.talkpojo.vo.BookShelfVO;
import com.cc.talkpojo.vo.BookShelfStatsVO;
import com.cc.talkserver.user.mapper.BookUserMapper;
import com.cc.talkserver.user.service.BookShelfService;
import com.cc.talkserver.user.mapper.BookShelfMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 个人书架服务实现类
 *
 * @author cc
 * @since 2025-10-12
 */
@Slf4j
@Service
public class BookShelfServiceImpl extends ServiceImpl<BookShelfMapper, BookShelf> implements BookShelfService {

    @Resource
    private BookUserMapper bookMapper;

    @Override
    public void addToShelf(BookShelfAddDTO addDTO) {
        // 获取当前用户ID (从SecurityContext或ThreadLocal获取)
        Long userId = getCurrentUserId();

        // 检查书籍是否已存在
        LambdaQueryWrapper<BookShelf> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(BookShelf::getUserId, userId)
                   .eq(BookShelf::getBookId, addDTO.getBookId());

        BookShelf existing = this.getOne(queryWrapper);
        if (existing != null) {
            throw new BaseException(BusinessConstant.BOOK_SHELF_ALREADY_EXISTS);
        }

        // 创建书架记录
        BookShelf bookShelf = new BookShelf();
        bookShelf.setUserId(userId);
        bookShelf.setBookId(addDTO.getBookId());
        bookShelf.setStatus(addDTO.getStatus());

        this.save(bookShelf);
        log.info("用户{}添加书籍{}到书架，状态: {}", userId, addDTO.getBookId(), addDTO.getStatus());
    }

    @Override
    public void removeFromShelf(Long shelfId) {
        Long userId = getCurrentUserId();

        LambdaQueryWrapper<BookShelf> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(BookShelf::getId, shelfId)
                   .eq(BookShelf::getUserId, userId);

        boolean result = this.remove(queryWrapper);
        if (!result) {
            throw new BaseException(BusinessConstant.BOOK_SHELF_ITEM_NOT_EXIST);
        }

        log.info("用户{}从书架移除书籍，书架ID: {}", userId, shelfId);
    }

    @Override
    public void updateStatus(Long shelfId, String status) {
        Long userId = getCurrentUserId();

        // 验证状态值
        if (!isValidStatus(status)) {
            throw new BaseException(BusinessConstant.BOOK_SHELF_STATUS_INVALID);
        }

        LambdaQueryWrapper<BookShelf> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(BookShelf::getId, shelfId)
                   .eq(BookShelf::getUserId, userId);

        BookShelf bookShelf = new BookShelf();
        bookShelf.setStatus(status);

        boolean result = this.update(bookShelf, queryWrapper);
        if (!result) {
            throw new BaseException(BusinessConstant.BOOK_SHELF_ITEM_NOT_EXIST);
        }

        log.info("用户{}更新书架状态，ID: {}, 状态: {}", userId, shelfId, status);
    }

    @Override
    public PageResult<BookShelfVO> getShelfList(BookShelfQueryDTO queryDTO) {
        Long userId = getCurrentUserId();

        // 构建查询条件
        LambdaQueryWrapper<BookShelf> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(BookShelf::getUserId, userId);

        // 状态筛选
        if (StringUtils.hasText(queryDTO.getStatus())) {
            queryWrapper.eq(BookShelf::getStatus, queryDTO.getStatus());
        }

        // 排序
        if ("CREATE_TIME".equals(queryDTO.getSortBy())) {
            queryWrapper.orderBy(true, "DESC".equals(queryDTO.getSortOrder()),
                               BookShelf::getCreateTime);
        } else if ("BOOK_NAME".equals(queryDTO.getSortBy())) {
            // 需要关联书籍表排序，这里简化处理
            queryWrapper.orderBy(true, "DESC".equals(queryDTO.getSortOrder()),
                               BookShelf::getCreateTime);
        }

        // 使用PageHelper进行分页
        PageHelper.startPage(queryDTO.getPage(), queryDTO.getSize());
        List<BookShelf> shelfList = this.list(queryWrapper);

        // 获取分页信息
        PageInfo<BookShelf> pageInfo = new PageInfo<>(shelfList);

        // 转换为VO（包含书名筛选）
        List<BookShelfVO> voList = convertToVOList(shelfList, queryDTO.getBookName());

        return new PageResult<>(pageInfo.getTotal(), voList);
    }

    @Override
    public BookShelfStatsVO getShelfStats() {
        Long userId = getCurrentUserId();

        LambdaQueryWrapper<BookShelf> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BookShelf::getUserId, userId);

        List<BookShelf> shelfList = this.list(wrapper);

        BookShelfStatsVO stats = new BookShelfStatsVO();

        for (BookShelf shelf : shelfList) {
            switch (shelf.getStatus()) {
                case "WANT_TO_READ":
                    stats.setWantToReadCount(stats.getWantToReadCount() + 1);
                    break;
                case "READING":
                    stats.setReadingCount(stats.getReadingCount() + 1);
                    break;
                case "READ":
                    stats.setReadCount(stats.getReadCount() + 1);
                    break;
            }
        }

        stats.setTotalCount(shelfList.size());

        return stats;
    }

    @Override
    public Boolean checkBookInShelf(Long bookId) {
        Long userId = getCurrentUserId();

        LambdaQueryWrapper<BookShelf> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(BookShelf::getUserId, userId)
                   .eq(BookShelf::getBookId, bookId);

        return this.count(queryWrapper) > 0;
    }

    /**
     * 转换实体列表为VO列表
     */
    private List<BookShelfVO> convertToVOList(List<BookShelf> shelfList, String bookNameFilter) {
        if (shelfList.isEmpty()) {
            return List.of();
        }

        // 获取书籍信息
        List<Long> bookIds = shelfList.stream()
                                    .map(BookShelf::getBookId)
                                    .collect(Collectors.toList());

        // 调用BookMapper获取书籍信息
        List<Book> books = bookMapper.selectBatchIds(bookIds);
        Map<Long, Book> bookMap = books.stream()
                                     .collect(Collectors.toMap(Book::getId, book -> book));

        // 转换为VO
        return shelfList.stream()
                      .map(shelf -> {
                          BookShelfVO vo = new BookShelfVO();
                          vo.setId(shelf.getId());
                          vo.setBookId(shelf.getBookId());
                          vo.setStatus(shelf.getStatus());
                          vo.setStatusDesc(getStatusDesc(shelf.getStatus()));
                          vo.setCreateTime(shelf.getCreateTime());
                          vo.setUpdateTime(shelf.getUpdateTime());

                          // 设置书籍信息
                          Book book = bookMap.get(shelf.getBookId());
                          if (book != null) {
                              vo.setBookName(book.getTitle());
                              vo.setBookCover(book.getCoverUrl());
                              vo.setAuthor(book.getAuthor());
                              vo.setPublisher(book.getPublisher());
                          }

                          return vo;
                      })
                      // 书名筛选
                      .filter(vo -> !StringUtils.hasText(bookNameFilter) ||
                                   vo.getBookName().contains(bookNameFilter))
                      .collect(Collectors.toList());
    }

    /**
     * 获取状态描述
     */
    private String getStatusDesc(String status) {
        switch (status) {
            case BusinessConstant.BOOK_SHELF_STATUS_WANT_TO_READ:
                return "想读";
            case BusinessConstant.BOOK_SHELF_STATUS_READING:
                return "在读";
            case BusinessConstant.BOOK_SHELF_STATUS_READ:
                return "读完";
            default:
                return "未知";
        }
    }

    /**
     * 验证状态是否有效
     */
    private boolean isValidStatus(String status) {
        return BusinessConstant.BOOK_SHELF_STATUS_WANT_TO_READ.equals(status) ||
               BusinessConstant.BOOK_SHELF_STATUS_READING.equals(status) ||
               BusinessConstant.BOOK_SHELF_STATUS_READ.equals(status);
    }

    /**
     * 获取当前用户ID
     */
    private Long getCurrentUserId() {
        return UserContext.getUser().getId();
    }
}
