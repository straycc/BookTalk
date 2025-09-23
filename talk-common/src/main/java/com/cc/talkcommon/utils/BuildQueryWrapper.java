package com.cc.talkcommon.utils;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.cc.talkpojo.dto.BookPageDTO;
import com.cc.talkpojo.entity.Book;


public class BuildQueryWrapper {

    /**
     * 构建查询条件包装器
     */
    public static LambdaQueryWrapper<Book> buildBookQueryWrapper(BookPageDTO bookPageDTO) {
        LambdaQueryWrapper<Book> wrapper = new LambdaQueryWrapper<>();

        // 字符串条件
        if (StringUtils.isNotBlank(bookPageDTO.getTitle())) {
            wrapper.like(Book::getTitle, bookPageDTO.getTitle());
        }
        if (StringUtils.isNotBlank(bookPageDTO.getAuthor())) {
            wrapper.like(Book::getAuthor, bookPageDTO.getAuthor());
        }
        if (StringUtils.isNotBlank(bookPageDTO.getIsbn())) {
            wrapper.eq(Book::getIsbn, bookPageDTO.getIsbn());
        }

        // 数值/状态条件
        if (bookPageDTO.getCategoryId() != null) {
            wrapper.eq(Book::getCategoryId, bookPageDTO.getCategoryId());
        }

        // 排序条件
        if (StringUtils.isNotBlank(bookPageDTO.getSortField())) {
            wrapper.orderBy(true,
                    !"desc".equalsIgnoreCase(bookPageDTO.getSortOrder()),
                    resolveSortField(bookPageDTO.getSortField()));
        }

        return wrapper;
    }
    /**
     * 安全解析排序字段（防止SQL注入）
     */
    private static SFunction<Book, ?> resolveSortField(String fieldName) {
        switch (fieldName) {
            case "title": return Book::getTitle;
            case "publishDate": return Book::getPublishDate;
            case "price": return Book::getPrice;
            default: return Book::getId; // 默认排序字段
        }
    }
}
