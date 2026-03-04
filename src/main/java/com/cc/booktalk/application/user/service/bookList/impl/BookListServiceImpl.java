package com.cc.booktalk.application.user.service.bookList.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.cc.booktalk.common.constant.BusinessConstant;
import com.cc.booktalk.common.constant.RedisCacheConstant;
import com.cc.booktalk.common.context.UserContext;
import com.cc.booktalk.common.exception.BaseException;
import com.cc.booktalk.common.utils.CheckPageParam;
import com.cc.booktalk.common.utils.ConvertUtils;
import com.cc.booktalk.entity.dto.bookList.BookListDTO;
import com.cc.booktalk.entity.dto.bookList.BookListPageDTO;
import com.cc.booktalk.entity.entity.book.Book;
import com.cc.booktalk.entity.entity.bookList.BookList;
import com.cc.booktalk.entity.entity.bookList.BookListItem;
import com.cc.booktalk.entity.entity.user.UserInfo;
import com.cc.booktalk.entity.result.PageResult;
import com.cc.booktalk.entity.vo.BookListDetailVO;
import com.cc.booktalk.entity.vo.BookListVO;
import com.cc.booktalk.entity.vo.BookShowVO;
import com.cc.booktalk.infrastructure.persistence.user.mapper.bookList.BookListItemMapper;
import com.cc.booktalk.infrastructure.persistence.user.mapper.bookList.BookListMapper;
import com.cc.booktalk.infrastructure.persistence.user.mapper.recommendation.UserInfoUserMapper;
import com.cc.booktalk.infrastructure.persistence.user.mapper.book.BookUserMapper;
import com.cc.booktalk.infrastructure.persistence.user.mapper.user.UserMapper;
import com.cc.booktalk.application.user.service.bookList.BookListItemService;
import com.cc.booktalk.application.user.service.bookList.BookListService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * <p>
 * 用户书单表 服务实现类
 * </p>
 *
 * @author cc
 * @since 2025-09-17
 */
@Service
public class BookListServiceImpl extends ServiceImpl<BookListMapper, BookList> implements BookListService {


    @Resource
    private BookListMapper bookListMapper;

    @Resource
    private BookListItemService bookListItemService;
    @Resource
    private BookListItemMapper bookListItemMapper;
    @Resource
    private UserMapper userMapper;

    private RedisTemplate<String, String> customStringRedisTemplate;

    @Resource
    private RedisTemplate<String, Object> customObjectRedisTemplate;
    @Resource
    private UserInfoUserMapper userInfoUserMapper;
    @Resource
    private BookUserMapper bookUserMapper;




    /**
     * 新建书单
     * @param bookListDTO
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createBookList(BookListDTO bookListDTO) {
        if (bookListDTO == null || bookListDTO.getUserId() == null) {
            throw new BaseException(BusinessConstant.PARAM_ERROR);
        }
        if (CollectionUtils.isEmpty(bookListDTO.getBookIdList())) {
            throw new BaseException(BusinessConstant.EMPTY_BOOKLIST);
        }

        // 统一时间
        LocalDateTime now = LocalDateTime.now();

        // 建立书单
        BookList bookList = ConvertUtils.convert(bookListDTO, BookList.class);
        bookList.setCreateTime(now);
        bookList.setUpdateTime(now);
        bookListMapper.insert(bookList);

        Long bookListId = bookList.getId();
        if (bookListId == null) {
            throw new BaseException(BusinessConstant.CREAT_BOOKLIST_ERROR);
        }

        // 建立书单-书籍关系
        AtomicInteger counter = new AtomicInteger(1);
        List<BookListItem> items = bookListDTO.getBookIdList().stream()
                .map(bookId -> {
                    BookListItem item = new BookListItem();
                    item.setBookListId(bookListId);
                    item.setBookId(bookId);
                    item.setSortOrder(counter.getAndIncrement()); // 按顺序递增
                    item.setCreateTime(now);
                    item.setUpdateTime(now);
                    return item;
                })
                .collect(Collectors.toList());
        // 批量保存
        bookListItemService.saveBatch(items);
    }


    /**
     * 修改书单（标题/描述）
     * @param bookListDTO
     */
    @Override
    @Transactional(rollbackFor = Exception.class)

    public void updateBookList(Long bookListId, BookListDTO bookListDTO) {
        if (bookListId == null || bookListDTO == null || bookListDTO.getUserId() == null) {
            throw new BaseException(BusinessConstant.PARAM_ERROR);
        }

        // 查询书单是否存在
        BookList bookList = bookListMapper.selectById(bookListId);
        if (bookList == null) {
            throw new BaseException(BusinessConstant.BOOKLIST_NOTEXIST);
        }

        // 校验是否当前用户的书单
        if (!Objects.equals(bookList.getUserId(), UserContext.getUser().getId())) {
            throw new BaseException(BusinessConstant.NOT_BOOKLIST_OWER);
        }

        // 统一时间
        LocalDateTime now = LocalDateTime.now();

        // 更新书单基本信息
        BookList bookListNew = BookList.builder()
                .id(bookListDTO.getBookListId())
                .title(bookListDTO.getTitle())
                .description(bookListDTO.getDescription())
                .updateTime(now)
                .build();
        bookListMapper.updateById(bookListNew);
    }


    /**
     * 删除书单
     * @param bookListId
     */
    @Override
    @Transactional
    public void deleteBookList(Long bookListId) {
        if (bookListId == null) {
            throw new BaseException(BusinessConstant.PARAM_ERROR);
        }
        BookList bookList = bookListMapper.selectById(bookListId);
        if (bookList == null) {
            throw new BaseException(BusinessConstant.BOOK_NOTEXIST);
        }
        if(!Objects.equals(bookList.getUserId(), UserContext.getUser().getId())){
            throw new BaseException(BusinessConstant.NOT_BOOKLIST_OWER);
        }
        // 删除书单-书籍关系表内容
        bookListItemMapper.delete(
                new QueryWrapper<BookListItem>().eq(BusinessConstant.SQL_BOOK_LIST_ID, bookListId)
        );
        // 删除书单表内容
        bookListMapper.deleteById(bookListId);
    }


    /**
     * 我的书单
     * @param bookListPageDTO
     * @return
     */
    @Override
    public PageResult<BookListVO> myBookListPage(BookListPageDTO bookListPageDTO) {
        CheckPageParam.checkPageDTO(bookListPageDTO);
        PageHelper.startPage(bookListPageDTO.getPageNum(), bookListPageDTO.getPageSize());
        // 构造查询条件
        LambdaQueryWrapper<BookList> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(BookList::getUserId, UserContext.getUser().getId());
        // 关键字模糊查询（书单标题 或 描述）
        if (bookListPageDTO.getKeyword() != null && !bookListPageDTO.getKeyword().trim().isEmpty()) {
            queryWrapper.and(q ->
                    q.like(BookList::getTitle, bookListPageDTO.getKeyword())
                            .or()
                            .like(BookList::getDescription, bookListPageDTO.getKeyword())
            );
        }

        // 排序逻辑
        if (bookListPageDTO.getSortBy() != null && !bookListPageDTO.getSortBy().trim().isEmpty()) {
            boolean isAsc = Boolean.TRUE.equals(bookListPageDTO.getAsc());

            // 这里可以通过字符串匹配你支持的排序字段
            switch (bookListPageDTO.getSortBy()) {
                case "create_time":
                    queryWrapper.orderBy(true, isAsc, BookList::getCreateTime);
                    break;
                case "update_time":
                    queryWrapper.orderBy(true, isAsc, BookList::getUpdateTime);
                    break;
                case "title":
                    queryWrapper.orderBy(true, isAsc, BookList::getTitle);
                    break;
                default:
                    // 默认按创建时间
                    queryWrapper.orderBy(true, false, BookList::getCreateTime);
                    break;
            }
        } else {
            // 默认按创建时间倒序
            queryWrapper.orderByDesc(BookList::getCreateTime);
        }

        // 分页查询
        List<BookList> bookLists = bookListMapper.selectList(queryWrapper);
        PageInfo<BookList> pageInfo = new PageInfo<>(bookLists);
        // 转换vo
        List<BookListVO> bookListVOs = bookLists.stream()
                .map(
                        bookList -> {
                            BookListVO bookListVO = new BookListVO();
                            // 查询书单创建者昵称
                            String infoKey = RedisCacheConstant.USER_INFO_KEY_PREFIX + bookList.getUserId();
                            UserInfo userInfo = (UserInfo) customObjectRedisTemplate.opsForValue().get(infoKey);
                            if(userInfo == null){
                                userInfo = userInfoUserMapper.selectOne(
                                        new LambdaQueryWrapper<UserInfo>().eq(UserInfo::getUserId, bookList.getUserId())
                                );
                            }
                            bookListVO.setNickName(userInfo.getNickname());
                            bookListVO.setTitle(bookList.getTitle());
                            bookList.setCoverUrl(bookList.getCoverUrl());
                            return bookListVO;
                        }
                ).collect(Collectors.toList());

        PageResult<BookListVO> pageResult = new PageResult<>();
        pageResult.setTotal(pageInfo.getTotal());
        pageResult.setRecords(bookListVOs);
        return pageResult;
    }


    /**
     * 查询书单详情
     * @param bookListId
     * @return
     */
    @Override
    public BookListDetailVO getBookListDetail(Long bookListId) {
        if(bookListId == null){
            throw new BaseException(BusinessConstant.PARAM_ERROR);
        }
        // 检查书单是否存在
        BookList bookList = bookListMapper.selectById(bookListId);
        if(bookList == null){
            throw new BaseException(BusinessConstant.BOOKLIST_NOTEXIST);
        }

        // 书单作者信息
        String infoKey = RedisCacheConstant.USER_INFO_KEY_PREFIX + bookList.getUserId();
        UserInfo userInfo = (UserInfo) customObjectRedisTemplate.opsForValue().get(infoKey);
        if(userInfo == null){
            userInfo = userInfoUserMapper.selectOne(
                    new LambdaQueryWrapper<UserInfo>().eq(UserInfo::getUserId, bookList.getUserId())
            );
            if(userInfo != null){
                customObjectRedisTemplate.opsForValue().set(infoKey, userInfo, 1, TimeUnit.DAYS);
            }
        }

        String nickname = userInfo.getNickname();
        String avatar = userInfo.getAvatarUrl();


        // 查询书单所有书籍id
        List<Long> bookIds = bookListItemMapper.selectList(
                        new LambdaQueryWrapper<BookListItem>().eq(BookListItem::getBookListId, bookListId)
                ).stream()
                .map(BookListItem::getBookId) // 只取出 bookId
                .collect(Collectors.toList());

        if(bookIds.isEmpty()){
            return new BookListDetailVO();//返回空数据
        }

        // 构造redis key
        List<String> bookKeys = bookIds.stream()
                .map(
                        bookId -> RedisCacheConstant.BOOK_DETAIL_KEY_PREFIX + bookId
                ).collect(Collectors.toList());

        // 批量查询 Redis
        List<Object> bookObjs = customObjectRedisTemplate.opsForValue().multiGet(bookKeys);

        // 准备容器
        Map<Long, Book> cachedBooks = new HashMap<>();
        List<Long> missingBookIds = new ArrayList<>();

        if(bookObjs != null && !bookObjs.isEmpty()){
            for (int i = 0; i < bookIds.size(); i++) {
                Long bookId = bookIds.get(i);
                Object obj = bookObjs.get(i);
                if (obj != null) {
                    cachedBooks.put(bookId, (Book) obj);
                } else {
                    missingBookIds.add(bookId);
                }
            }
        } else {
            // 全部未命中
            missingBookIds.addAll(bookIds);
        }

        // 批量查询 MySQL
        if(!missingBookIds.isEmpty()) {
            List<Book> dbBooks = bookUserMapper.selectBatchIds(missingBookIds);
            for(Book book : dbBooks){
                cachedBooks.put(book.getId(), book);
                // 写回 Redis 缓存，设置过期时间
                String key = RedisCacheConstant.BOOK_DETAIL_KEY_PREFIX + book.getId();
                customObjectRedisTemplate.opsForValue().set(key, book, 1, TimeUnit.DAYS);
            }
        }

        // 组装结果，保持和 bookIds 顺序一致
        List<BookShowVO> bookShowVOs = new ArrayList<>(bookIds.size());
        for (Long bookId : bookIds) {
            Book book = cachedBooks.get(bookId);
            if (book != null) {
                BookShowVO vo = new BookShowVO();
                // 属性拷贝
                BeanUtils.copyProperties(book, vo);
                bookShowVOs.add(vo);
            }
        }


        return BookListDetailVO.builder()
                .title(bookList.getTitle())
                .description(bookList.getDescription())
                .coverUrl(bookList.getCoverUrl())
                .nickName(nickname)
                .avatar(avatar)
                .bookShows(bookShowVOs)
                .updateTime(bookList.getUpdateTime())
                .build();
    }

    /**
     * 书单新增书籍
     * @param bookListId
     * @param bookListDTO
     */
    @Override
    public void addBook(Long bookListId, BookListDTO bookListDTO) {
        if(bookListId == null || bookListDTO == null ){
            throw new BaseException(BusinessConstant.PARAM_ERROR);
        }
        if(bookListDTO.getBookId() == null ){
            return ;
        }
        // 检验书单是否存在以及创建者
        BookList bookList = bookListMapper.selectById(bookListDTO.getBookListId());
        if(bookList == null){
            throw new BaseException(BusinessConstant.BOOKLIST_NOTEXIST);
        }
        if(!bookList.getUserId().equals(UserContext.getUser().getId())){
            throw new BaseException(BusinessConstant.NOT_BOOKLIST_OWER);
        }

        // 查询增加的书籍信息
        Book book = bookUserMapper.selectOne(
                new LambdaQueryWrapper<Book>().eq(Book::getId,bookListDTO.getBookId())
        );
        if(book == null){
            throw new BaseException(BusinessConstant.BOOK_NOTEXIST);
        }
        // 查询书籍是否已经在书单中
        BookListItem bookListItem = bookListItemMapper.selectOne(
                new LambdaQueryWrapper<BookListItem>().eq(BookListItem::getBookId,bookListDTO.getBookId())
        );
        if(bookListItem != null){
            return;
        }

        // 查询当前书单最大 sortOrder
        Integer maxSort = bookListItemMapper.selectObjs(
                        new QueryWrapper<BookListItem>()
                                .eq("book_list_id", bookListId)
                                .select("MAX(sort_order)")
                ).stream()
                .findFirst()
                .map(o -> (Integer)o)
                .orElse(0);

        LocalDateTime now = LocalDateTime.now();
        // 添加书籍信息
        BookListItem bookListItemN = BookListItem.builder()
                .bookListId(bookListId)
                .bookId(book.getId())
                .sortOrder(maxSort+1)
                .createTime(now)
                .updateTime(now)
                .build();
        bookListItemMapper.insert(bookListItemN);
    }

    /**
     * 书单删除书籍
     * @param bookListId
     * @param bookListDTO
     */
    @Override
    public void deleteBook(Long bookListId, BookListDTO bookListDTO) {
        if(bookListId == null || bookListDTO == null ){
            throw new BaseException(BusinessConstant.PARAM_ERROR);
        }
        if(bookListDTO.getBookId() == null ){
            return ;
        }
        // 检验书单是否存在以及创建者
        BookList bookList = bookListMapper.selectById(bookListDTO.getBookListId());
        if(bookList == null){
            throw new BaseException(BusinessConstant.BOOKLIST_NOTEXIST);
        }
        if(!bookList.getUserId().equals(UserContext.getUser().getId())){
            throw new BaseException(BusinessConstant.NOT_BOOKLIST_OWER);
        }
        // 查询删除的书籍是否在书单中
        BookListItem bookListItem = bookListItemMapper.selectOne(
                new LambdaQueryWrapper<BookListItem>().eq(BookListItem::getBookId,bookListDTO.getBookId())
        );
        if(bookListItem == null){
            throw new BaseException(BusinessConstant.BOOKLIST_NOTEXIST_BOOK);
        }
        // 删除书单中书籍
        bookListItemMapper.delete(
                new LambdaQueryWrapper<BookListItem>().eq(BookListItem::getBookId,bookListDTO.getBookId())
        );
    }




}
