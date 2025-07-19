package com.cc.talkserver.user.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cc.talkcommon.constant.BusinessConstant;
import com.cc.talkcommon.constant.ElasticsearchConstant;
import com.cc.talkcommon.constant.RedisCacheConstant;
import com.cc.talkcommon.exception.BaseException;
import com.cc.talkcommon.result.Result;
import com.cc.talkcommon.utils.BuildQueryWrapper;
import com.cc.talkcommon.utils.CheckPageParam;
import com.cc.talkcommon.utils.ConvertUtils;
import com.cc.talkpojo.Result.PageResult;
import com.cc.talkpojo.dto.BookShowDTO;
import com.cc.talkpojo.dto.PageBookDTO;
import com.cc.talkpojo.dto.PageSearchDTO;
import com.cc.talkpojo.entity.Book;
import com.cc.talkpojo.entity.BookCategory;
import com.cc.talkpojo.entity.BookES;
import com.cc.talkpojo.entity.BookTagRelation;
import com.cc.talkpojo.vo.BookVO;
import com.cc.talkpojo.vo.CategoryVO;
import com.cc.talkserver.user.mapper.BookTagUserMapper;
import com.cc.talkserver.user.mapper.BookUserMapper;
import com.cc.talkserver.user.mapper.CategoryUserMapper;
import com.cc.talkserver.user.service.BookUserService;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;

import com.github.pagehelper.PageInfo;
import lombok.NonNull;
import org.apache.coyote.Response;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.naming.directory.SearchResult;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * 图书主表 服务实现类
 * </p>
 *
 * @author cc
 * @since 2025-06-30
 */
@Service
public class BookUserServiceImpl extends ServiceImpl<BookUserMapper, Book> implements BookUserService {

    @Resource
    private BookUserMapper bookUserMapper;

    @Resource
    private CategoryUserMapper categoryUserMapper;

    @Resource
    private BookTagUserMapper bookTagUserMapper;

    @Resource
    private ElasticsearchClient elasticsearchClient;


    @Resource
    @Qualifier("customStringRedisTemplate")
    private RedisTemplate<String, String> stringRedisTemplate;

    @Resource
    @Qualifier("customObjectRedisTemplate")
    private RedisTemplate<String, Object> objectRedisTemplate;


    /**
     * 图书搜索（书名/作者/ISBN）
     * @param pageSearchDTO
     * @return
     */
    @Override
    public PageResult<BookShowDTO> getSearchPage(PageSearchDTO pageSearchDTO) {

        CheckPageParam.checkPageDTO(pageSearchDTO);
        String keyword = pageSearchDTO.getKeyword();
        int page = pageSearchDTO.getPageNum();
        int size = pageSearchDTO.getPageSize();

        //1. 构建ES查询条件
        SearchRequest searchRequest = SearchRequest.of(s->s
                .index(ElasticsearchConstant.ES_BOOK_INDEX)
                .from((page-1)*size)
                .size(size)
                .query(q->q
                        .multiMatch(m->m
                                .query(keyword)
                                .fields("title","author","isbn")
                        )
                )//query
        );
        //2. 进行搜索
        try{
            SearchResponse<BookES> searchResponse = elasticsearchClient.search(searchRequest,BookES.class);
            List<BookShowDTO> bookShowList = new ArrayList<>();
            for(Hit<BookES> hit : searchResponse.hits().hits()){
                BookES bookES = hit.source();
                if(bookES!=null){
                    BookShowDTO showDTO = ConvertUtils.convert(bookES, BookShowDTO.class);
                    bookShowList.add(showDTO);
                }
            }
            // 3. 获取总记录数
            long total = searchResponse.hits().total() != null ?
                    searchResponse.hits().total().value() : 0;
            return new PageResult<BookShowDTO>(total,bookShowList);
        }
        catch(Exception e){
            log.error("ES搜索失败", e);
            throw new BaseException(BusinessConstant.BOOK_SEARCH_ERROR);
        }
    }


    /**
     * 图书分页查询展示
     * @param pageBookDTO
     * @return
     */
    @Override
    public PageResult<BookShowDTO> getBookPage(PageBookDTO pageBookDTO) {
        // 1. 参数检查
        CheckPageParam.checkPageDTO(pageBookDTO);

        // 2. 构建查询条件
        LambdaQueryWrapper<Book> wrapper = BuildQueryWrapper.buildBookQueryWrapper(pageBookDTO);

        // 3. 执行分页查询
        PageHelper.startPage(pageBookDTO.getPageNum(), pageBookDTO.getPageSize());
        List<Book> booksList = bookUserMapper.selectList(wrapper);

        // 更安全的获取分页信息方式
        PageInfo<Book> pageInfo = new PageInfo<>(booksList);

        // 4. 转换并检查空值
        List<BookShowDTO> voList = booksList.stream()
                .filter(Objects::nonNull)
                .map(book -> ConvertUtils.convert(book, BookShowDTO.class))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // 5. 返回结果
        return new PageResult<>(pageInfo.getTotal(), voList);
    }


    /**
     * 获取书籍详情
     * @param id
     * @return
     */
    @Override
    public BookVO getBookDetail(Long id) {
        if (id == null || id <= 0) {
            throw new BaseException(BusinessConstant.PARAM_ERROR);
        }
        Book book = bookUserMapper.selectById(id);
        if (book == null) {
            throw new BaseException(BusinessConstant.BOOK_NOTEXIST);
        }
        BookVO bookVO = ConvertUtils.convert(book, BookVO.class);
        //查询所属分类
        BookCategory bookCategory = categoryUserMapper.selectById(book.getCategoryId());
        if(bookCategory != null){
            bookVO.setCategoryName(bookCategory.getName());
        }
        return bookVO;
    }


    /**
     * 获取分类列表
     * @return
     */
    @Override
    public List<CategoryVO> getCategoryList() {
        List<BookCategory> list = categoryUserMapper.selectList(null);
        return list.stream().map(
                bookCategory -> ConvertUtils.convert(bookCategory,CategoryVO.class)
        ).collect(Collectors.toList());
    }

    /**
     * 根据标签分页查询书籍
     * @param id
     * @return
     */
    @Override
    public PageResult<BookShowDTO> getPageByTag(Integer id,PageBookDTO pageBookDTO) {

        // 1. 参数检查
        CheckPageParam.checkPageDTO(pageBookDTO);

        // 2. 查询与标签关联的图书 ID
        LambdaQueryWrapper<BookTagRelation> relationWrapper = new LambdaQueryWrapper<>();
        relationWrapper.eq(BookTagRelation::getTagId, id);
        List<BookTagRelation> relations = bookTagUserMapper.selectList(relationWrapper);
        List<Long> bookIds = relations.stream()
                .map(BookTagRelation::getBookId)
                .distinct()
                .collect(Collectors.toList());

        if (bookIds.isEmpty()) {
            return new PageResult<>(0L, Collections.emptyList());
        }

        // 3. 分页查询图书信息
        PageHelper.startPage(pageBookDTO.getPageNum(), pageBookDTO.getPageSize());
        List<Book> books = bookUserMapper.selectBatchIds(bookIds);  // 或根据顺序查询自定义SQL

        Page<Book> pageInfo = (Page<Book>) books;

        // 4. 转换为 VO 列表
        List<BookShowDTO> voList = books.stream()
                .map(book -> ConvertUtils.convert(book, BookShowDTO.class))
                .collect(Collectors.toList());

        // 5. 返回结果
        return new PageResult<>(pageInfo.getTotal(), voList);
    }


    /**
     * 查询缓存热门书籍
     * @param pageBookDTO
     * @return
     */
    @Override
    public PageResult<BookShowDTO> getHotBook(PageBookDTO pageBookDTO) {

        //1. 参数检查
        CheckPageParam.checkPageDTO(pageBookDTO);
        //2. 获取必要参数
        Long tagId = pageBookDTO.getTagId();
        int pageNum = pageBookDTO.getPageNum();
        int pageSize = pageBookDTO.getPageSize();
        int start = (pageNum - 1) * pageSize;
        int end = start + pageSize - 1;

        //3. 构造 Redis key
        String month = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        String listKey = RedisCacheConstant.HOT_BOOKS_KEY_PREFIX + month + ":" + tagId; //bookId列表
        String detailKey = RedisCacheConstant.BOOK_DETAIL_KEY_PREFIX + tagId; //BookDetail哈希表

        //4. Redis 获取图书 ID 列表
        List<String> bookIdList = stringRedisTemplate.opsForList().range(listKey,start,end);
        if (bookIdList == null || bookIdList.isEmpty()) {
            return new PageResult<>(0L, Collections.emptyList());
        }
        //5. 根据BookId 获取图书详情
        List<BookShowDTO> bookShowDTOS = bookIdList.stream().map(
                bookId -> {
                    Object bookObj = objectRedisTemplate.opsForHash().get(detailKey,bookId);
                    if(bookObj != null){
                        return ConvertUtils.convert(bookObj,BookShowDTO.class);
                    }
                    return null;
                }
        ) .filter(Objects::nonNull).collect(Collectors.toList());

        //6. 返回分页结果
        long total = Optional.ofNullable(// 防止返回null，触发空指针异常
                stringRedisTemplate.opsForList().size(listKey)
        ).orElse(0L);
        return new PageResult<>(total, bookShowDTOS);

    }


}
