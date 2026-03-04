package com.cc.booktalk.application.user.service.bookList.impl;

import com.cc.booktalk.entity.entity.bookList.BookListItem;
import com.cc.booktalk.infrastructure.persistence.user.mapper.bookList.BookListItemMapper;
import com.cc.booktalk.application.user.service.bookList.BookListItemService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 书单与图书关联表 服务实现类
 * </p>
 *
 * @author cc
 * @since 2025-09-19
 */
@Service
public class BookListItemServiceImpl extends ServiceImpl<BookListItemMapper, BookListItem> implements BookListItemService {

}
