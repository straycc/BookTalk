package com.cc.booktalk.application.user.service.bookShelf.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cc.booktalk.common.constant.BusinessConstant;
import com.cc.booktalk.common.context.UserContext;
import com.cc.booktalk.common.exception.BaseException;
import com.cc.booktalk.interfaces.dto.user.bookShelf.BookShelfAddDTO;
import com.cc.booktalk.interfaces.dto.user.bookShelf.BookShelfQueryDTO;
import com.cc.booktalk.domain.entity.bookShelf.BookShelf;
import com.cc.booktalk.common.result.PageResult;
import com.cc.booktalk.interfaces.vo.user.bookShelf.BookShelfVO;
import com.cc.booktalk.interfaces.vo.user.bookShelf.BookShelfStatsVO;
import com.cc.booktalk.application.user.service.bookShelf.BookShelfService;
import com.cc.booktalk.infrastructure.persistence.user.mapper.bookShelf.BookShelfMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.List;

/**
 * 个人书架服务实现类
 *
 * @author cc
 * @since 2025-10-12
 */
@Slf4j
@Service
public class BookShelfServiceImpl extends ServiceImpl<BookShelfMapper, BookShelf> implements BookShelfService {

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
        bookShelf.setUpdateTime(java.time.LocalDateTime.now());

        boolean result = this.update(bookShelf, queryWrapper);
        if (!result) {
            throw new BaseException(BusinessConstant.BOOK_SHELF_ITEM_NOT_EXIST);
        }

        log.info("用户{}更新书架状态，ID: {}, 状态: {}", userId, shelfId, status);
    }

    @Override
    public PageResult<BookShelfVO> getShelfList(BookShelfQueryDTO queryDTO) {
        if (queryDTO == null) {
            throw new BaseException(BusinessConstant.PARAM_ERROR);
        }
        Long userId = getCurrentUserId();
        PageHelper.startPage(queryDTO.getPage(), queryDTO.getSize());
        List<BookShelfVO> shelfList = baseMapper.selectShelfPage(
                userId,
                normalizeStatus(queryDTO.getStatus()),
                queryDTO.getBookName(),
                "BOOK_NAME".equals(queryDTO.getSortBy()) ? "BOOK_NAME" : "CREATE_TIME",
                "ASC".equals(queryDTO.getSortOrder()) ? "ASC" : "DESC"
        );
        shelfList.forEach(item -> item.setStatusDesc(getStatusDesc(item.getStatus())));
        PageInfo<BookShelfVO> pageInfo = new PageInfo<>(shelfList);
        return new PageResult<>(pageInfo.getTotal(), shelfList);
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
                    java.time.LocalDateTime completedAt = shelf.getUpdateTime() == null
                            ? shelf.getCreateTime() : shelf.getUpdateTime();
                    if (completedAt != null && completedAt.getYear() == java.time.Year.now().getValue()) {
                        stats.setYearlyReadCount(stats.getYearlyReadCount() + 1);
                    }
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

    private String normalizeStatus(String status) {
        return StringUtils.hasText(status) && !"null".equalsIgnoreCase(status) ? status : null;
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
