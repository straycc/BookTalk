package com.cc.talkadmin.service.impl;

import com.cc.talkpojo.entity.BookCategory;
import com.cc.talkadmin.mapper.BookCategoryMapper;
import com.cc.talkadmin.service.IBookCategoryService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 图书分类表 服务实现类
 * </p>
 *
 * @author cc
 * @since 2025-07-07
 */
@Service
public class BookCategoryServiceImpl extends ServiceImpl<BookCategoryMapper, BookCategory> implements IBookCategoryService {

}
