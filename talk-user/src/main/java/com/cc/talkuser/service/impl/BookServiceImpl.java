package com.cc.talkuser.service.impl;

import com.cc.talkpojo.entity.Book;
import com.cc.talkuser.mapper.BookMapper;
import com.cc.talkuser.service.IBookService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 图书主表 服务实现类
 * </p>
 *
 * @author cc
 * @since 2025-06-30
 */
@Service
public class BookServiceImpl extends ServiceImpl<BookMapper, Book> implements IBookService {

}
