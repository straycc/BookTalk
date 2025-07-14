package com.cc.talkcommon.utils;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.cc.talkpojo.dto.PageBookDTO;
import com.cc.talkpojo.entity.Book;


public class BuildQueryWrapper {

    /**
     * 构建查询条件包装器
     */
    public static LambdaQueryWrapper<Book> buildBookQueryWrapper(PageBookDTO pageBookDTO) {
        LambdaQueryWrapper<Book> wrapper = new LambdaQueryWrapper<>();

        // 字符串条件
        if (StringUtils.isNotBlank(pageBookDTO.getTitle())) {
            wrapper.like(Book::getTitle, pageBookDTO.getTitle());
        }
        if (StringUtils.isNotBlank(pageBookDTO.getAuthor())) {
            wrapper.like(Book::getAuthor, pageBookDTO.getAuthor());
        }
        if (StringUtils.isNotBlank(pageBookDTO.getIsbn())) {
            wrapper.eq(Book::getIsbn, pageBookDTO.getIsbn());
        }

        // 数值/状态条件
        if (pageBookDTO.getCategoryId() != null) {
            wrapper.eq(Book::getCategoryId, pageBookDTO.getCategoryId());
        }
        // 排序条件
        if (StringUtils.isNotBlank(pageBookDTO.getSortField())) {
            wrapper.orderBy(true,
                    !"desc".equalsIgnoreCase(pageBookDTO.getSortOrder()),
                    resolveSortField(pageBookDTO.getSortField()));
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
