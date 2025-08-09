package com.cc.talkserver.user.service.impl;

import cn.hutool.core.lang.TypeReference;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
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
import com.cc.talkcommon.redis.RedisData;
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
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
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
@Slf4j
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
    private RedissonClient redissonClient;

    @Resource
    private RedisTemplate<String, String> customStringRedisTemplate;

    @Resource
    private RedisTemplate<String, Object> customObjectRedisTemplate;


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

        String hashKey = RedisCacheConstant.BOOK_DETAIL_KEY_PREFIX;
        String fieldKey = String.valueOf(id);

        Object bookObj = customObjectRedisTemplate.opsForHash().get(hashKey, fieldKey);

        // 1. 命中缓存
        if (bookObj != null) {
            String json = bookObj.toString();
            if (StrUtil.isNotBlank(json)) {
                RedisData<BookVO> redisData = JSONUtil.toBean(json, new TypeReference<RedisData<BookVO>>() {}, false);
                LocalDateTime expireTime = redisData.getExpireTime();
                BookVO bookVO = redisData.getData();

                // 1.1 缓存未过期，直接返回
                if (expireTime != null && expireTime.isAfter(LocalDateTime.now())) {
                    return bookVO;
                }

                // 1.2 缓存已过期，尝试重建缓存（逻辑过期 + 分布式锁 + 双检缓存）
                String lockKey = RedisCacheConstant.REDISSION_BOOKDETAIL_LOCK_PREFIX + fieldKey;
                RLock lock = redissonClient.getLock(lockKey);
                boolean locked = false;

                try {
                    locked = lock.tryLock(0, 10, TimeUnit.SECONDS);
                    if (locked) {
                        // 再次确认缓存是否已被其他线程重建
                        Object doubleCheck = customObjectRedisTemplate.opsForHash().get(hashKey, fieldKey);
                        if (doubleCheck != null && StrUtil.isNotBlank(doubleCheck.toString())) {
                            RedisData<BookVO> freshCache = JSONUtil.toBean(doubleCheck.toString(), new TypeReference<RedisData<BookVO>>() {}, false);
                            if (freshCache.getExpireTime() != null && freshCache.getExpireTime().isAfter(LocalDateTime.now())) {
                                return freshCache.getData();
                            }
                        }

                        // 缓存未重建 → 查数据库 + 写缓存
                        Book book = bookUserMapper.selectById(id);
                        if (book == null) {
                            // 写入空值，防止击穿
                            customObjectRedisTemplate.opsForHash().put(hashKey, fieldKey, "");
                            customObjectRedisTemplate.expire(hashKey, Duration.ofMinutes(10));
                            throw new BaseException(BusinessConstant.BOOK_NOTEXIST);
                        }

                        BookVO freshVO = ConvertUtils.convert(book, BookVO.class);
                        fillCategoryName(freshVO);

                        RedisData<BookVO> freshData = new RedisData<>();
                        freshData.setData(freshVO);
                        freshData.setExpireTime(LocalDateTime.now().plusDays(BusinessConstant.BOOK_CACAHE_EXPIRETIME));

                        customObjectRedisTemplate.opsForHash().put(hashKey, fieldKey, JSONUtil.toJsonStr(freshData));
                        return freshVO;
                    } else {
                        // 未获取到锁 → 返回旧数据
                        return bookVO;
                    }
                } catch (InterruptedException e) {
                    log.error("获取分布式锁失败: {}", e.getMessage());
                    throw new BaseException(BusinessConstant.TRYLOCK_ERROR);
                } finally {
                    if (locked) lock.unlock();
                }
            } else {
                // 空值缓存 → 数据库中也没有
                throw new BaseException(BusinessConstant.BOOK_NOTEXIST);
            }
        }

        // 2. 缓存未命中 → 查数据库
        Book book = bookUserMapper.selectById(id);
        if (book == null) {
            // 缓存空值，防击穿
            customObjectRedisTemplate.opsForHash().put(hashKey, fieldKey, "");
            customObjectRedisTemplate.expire(hashKey, Duration.ofMinutes(10));
            throw new BaseException(BusinessConstant.BOOK_NOTEXIST);
        }

        BookVO bookVO = ConvertUtils.convert(book, BookVO.class);
        fillCategoryName(bookVO);

        RedisData<BookVO> redisData = new RedisData<>();
        redisData.setData(bookVO);
        redisData.setExpireTime(LocalDateTime.now().plusDays(BusinessConstant.BOOK_CACAHE_EXPIRETIME));

        customObjectRedisTemplate.opsForHash().put(hashKey, fieldKey, JSONUtil.toJsonStr(redisData));
        return bookVO;
    }



    /**
     * 填充分类名
     * @param bookVO
     */
    private void fillCategoryName(BookVO bookVO) {
        BookCategory bookCategory = categoryUserMapper.selectById(bookVO.getId());
        if (bookCategory != null) {
            bookVO.setCategoryName(bookCategory.getName());
        }
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
     *
     */

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
        List<String> bookIdList = customStringRedisTemplate.opsForList().range(listKey,start,end);
        if (bookIdList == null || bookIdList.isEmpty()) {
            return new PageResult<>(0L, Collections.emptyList());
        }
        //5. 根据BookId 获取图书详情
        List<BookShowDTO> bookShowDTOS = bookIdList.stream().map(
                bookId -> {
                    Object bookObj = customObjectRedisTemplate.opsForHash().get(detailKey,bookId);
                    if(bookObj != null){
                        return ConvertUtils.convert(bookObj,BookShowDTO.class);
                    }
                    return null;
                }
        ) .filter(Objects::nonNull).collect(Collectors.toList());

        //6. 返回分页结果
        long total = Optional.ofNullable(// 防止返回null，触发空指针异常
                customStringRedisTemplate.opsForList().size(listKey)
        ).orElse(0L);
        return new PageResult<>(total, bookShowDTOS);

    }


}
