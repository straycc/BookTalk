package com.cc.talkserver.user.service.impl;

import com.cc.talkpojo.entity.BookListItem;
import com.cc.talkpojo.entity.BookListItem;
import com.cc.talkserver.user.mapper.BookListItemMapper;
import com.cc.talkserver.user.service.BookListItemService;
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
