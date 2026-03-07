package com.cc.booktalk.application.user.service.book.impl;

import cn.hutool.core.lang.TypeReference;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.FunctionBoostMode;
import co.elastic.clients.elasticsearch._types.query_dsl.FunctionScoreMode;
import co.elastic.clients.elasticsearch._types.query_dsl.FieldValueFactorModifier;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.util.ObjectBuilder;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cc.booktalk.common.constant.BusinessConstant;
import com.cc.booktalk.common.constant.ElasticsearchConstant;
import com.cc.booktalk.common.constant.RedisCacheConstant;
import com.cc.booktalk.common.exception.BaseException;
import com.cc.booktalk.common.redis.RedisData;
import com.cc.booktalk.common.utils.BuildQueryWrapper;
import com.cc.booktalk.common.utils.CheckPageParam;
import com.cc.booktalk.common.utils.ConvertUtils;
import com.cc.booktalk.common.result.PageResult;
import com.cc.booktalk.interfaces.dto.user.book.BookShowDTO;
import com.cc.booktalk.interfaces.dto.user.book.BookPageDTO;
import com.cc.booktalk.interfaces.dto.user.search.PageSearchDTO;
import com.cc.booktalk.domain.entity.book.Book;
import com.cc.booktalk.domain.entity.category.Category;
import com.cc.booktalk.domain.entity.book.BookES;
import com.cc.booktalk.domain.entity.tag.BookTagRelation;
import com.cc.booktalk.interfaces.vo.user.book.BookVO;
import com.cc.booktalk.interfaces.vo.user.category.CategoryVO;
import com.cc.booktalk.infrastructure.persistence.user.mapper.tag.BookTagUserMapper;
import com.cc.booktalk.infrastructure.persistence.user.mapper.book.BookUserMapper;
import com.cc.booktalk.infrastructure.persistence.user.mapper.category.CategoryUserMapper;
import com.cc.booktalk.application.user.service.book.BookUserService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
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
        final String keyword = pageSearchDTO.getKeyword().trim();
        if (StrUtil.isBlank(keyword)) {
            throw new BaseException(BusinessConstant.PARAM_ERROR);
        }


        int page = pageSearchDTO.getPageNum();
        int size = pageSearchDTO.getPageSize();

        // 1. 构建ES查询条件：ISBN精确匹配；普通关键词使用文本相关性+业务热度混排
        SearchRequest searchRequest;
        if (isIsbnKeyword(keyword)) {
            String normalizedIsbn = keyword.replace("-", "");
            searchRequest = SearchRequest.of(s -> s
                    .index(ElasticsearchConstant.ES_BOOK_INDEX)
                    .from((page - 1) * size)
                    .size(size)
                    .query(q -> q.term(t -> t.field("isbn").value(normalizedIsbn)))
            );
        } else {
            List<String> terms = splitSearchTerms(keyword);

            searchRequest = SearchRequest.of(s -> s
                    .index(ElasticsearchConstant.ES_BOOK_INDEX)
                    .from((page - 1) * size)
                    .size(size)
                    .query(q -> q.functionScore(fs -> fs
                            .query(iq -> buildKeywordQuery(iq, keyword, terms))
                            .functions(f -> f.fieldValueFactor(fvf -> fvf
                                    .field("hotScore")
                                    .factor(0.01)
                                    .missing(0.0)
                            ))
                            .functions(f -> f.fieldValueFactor(fvf -> fvf
                                    .field("favoriteCount")
                                    .modifier(FieldValueFactorModifier.Log1p)
                                    .factor(0.05)
                                    .missing(0.0)
                            ))
                            .functions(f -> f.fieldValueFactor(fvf -> fvf
                                    .field("scoreCount")
                                    .modifier(FieldValueFactorModifier.Log1p)
                                    .factor(0.05)
                                    .missing(0.0)
                            ))
                            .scoreMode(FunctionScoreMode.Sum)
                            .boostMode(FunctionBoostMode.Sum)
                    ))
            );
        }
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
     * 关键词匹配策略
     * @param queryBuilder
     * @param keyword
     * @param terms
     * @return
     */
    private ObjectBuilder<Query> buildKeywordQuery(
            Query.Builder queryBuilder,
            String keyword,
            List<String> terms
    ) {
        if (terms.size() <= 1) {// 但单关键字，直接简单匹配
            return queryBuilder.multiMatch(m -> m
                    .query(keyword)
                    .fields("title^4", "subTitle^2", "author^2")
                    .minimumShouldMatch("1")//至少满足一个
            );
        }

        return queryBuilder.bool(b -> {
            for (String term : terms) {// 多关键字分别匹配，并通过 or 连接
                b.should(sh -> sh.multiMatch(m -> m
                        .query(term)
                        .fields("title^4", "subTitle^2", "author^2")
                        .minimumShouldMatch("1")
                ));
            }
            b.should(sh -> sh.matchPhrase(mp -> mp.field("title").query(keyword).boost(4.0f)));
            return b.minimumShouldMatch("1");
        });
    }

    /**
     * 按照常用的标点分割符分割关键字
     * @param keyword
     * @return
     */
    private List<String> splitSearchTerms(String keyword) {
        return Arrays.stream(keyword.split("[\\s,，。；;、/|+]+"))
                    .filter(StrUtil::isNotBlank)
                    .collect(Collectors.toList());
    }

    /**
     * 判断输入是否是ISBN（支持10/13位，允许中划线，10位末位可为X）
     */
    private boolean isIsbnKeyword(String keyword) {
        if (StrUtil.isBlank(keyword)) {
            return false;
        }
        String normalized = keyword.replace("-", "").trim();
        return normalized.matches("^[0-9]{13}$") || normalized.matches("^[0-9]{9}[0-9Xx]$");
    }


    /**
     * 图书分页查询展示
     * @param bookPageDTO
     * @return
     */
    @Override
    public PageResult<BookShowDTO> getBookPage(BookPageDTO bookPageDTO) {
        // 1. 参数检查
        CheckPageParam.checkPageDTO(bookPageDTO);

        // 2. 构建查询条件
        LambdaQueryWrapper<Book> wrapper = BuildQueryWrapper.buildBookQueryWrapper(bookPageDTO);

        // 3. 执行分页查询
        PageHelper.startPage(bookPageDTO.getPageNum(), bookPageDTO.getPageSize());
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
        if (bookVO.getCategoryId() != null) {
            Category category = categoryUserMapper.selectById(bookVO.getCategoryId());
            if (category != null) {
                bookVO.setCategoryName(category.getName());
            }
        }
    }


    /**
     * 获取分类列表
     * @return
     */
    @Override
    public List<CategoryVO> getCategoryList() {
        List<Category> list = categoryUserMapper.selectList(null);
        return list.stream().map(
                bookCategory -> ConvertUtils.convert(bookCategory,CategoryVO.class)
        ).collect(Collectors.toList());
    }

    /**
     * 根据标签分页查询书籍(查询ES)
     * @param id
     * @return
     */
    @Override
    public PageResult<BookShowDTO> getPageByTag(Long id, BookPageDTO bookPageDTO) {

        // 1. 参数检查
        CheckPageParam.checkPageDTO(bookPageDTO);

        // 2. 查询与标签关联的图书
        int from = (bookPageDTO.getPageNum() - 1) * bookPageDTO.getPageSize();
        int size = bookPageDTO.getPageSize();
        // 查询book_tag_index
        try {
            SearchResponse<BookTagRelation> relationResp = elasticsearchClient.search(s -> s
                            .index(ElasticsearchConstant.ES_BOOK_TAG_INDEX)
                            .query(q -> q.term(t -> t.field("tagId.keyword").value(bookPageDTO.getTagId())))
                            .from(from)
                            .size(size)
                            .sort(sort -> sort.field(f -> f.field("bookId.keyword").order(SortOrder.Asc))),
                    BookTagRelation.class
            );
            // 获取bookId列表
            List<Long> bookIds = relationResp.hits().hits().stream()
                    .map(hit -> hit.source().getBookId())
                    .distinct()
                    .collect(Collectors.toList());


            long total = relationResp.hits().total() !=null ? relationResp.hits().total().value() : 0L;
            if(bookIds.isEmpty()){
                //未查询到bookIds,返回空列表
                return new PageResult<>(0L,Collections.emptyList());
            }

            //3. 查询book_index获取图信息
            SearchResponse<BookES> bookResp = elasticsearchClient.search(
                    s->s.index(ElasticsearchConstant.ES_BOOK_INDEX)
                            .query(q -> q.terms(t -> t.field("id").terms(ts -> ts.value(
                                bookIds.stream().map(v -> FieldValue.of(v)).collect(Collectors.toList())
                            ))))
                            .size(size),
                    BookES.class
            );

            //4. 返回结果
            List<BookShowDTO> books = bookResp.hits().hits().stream()
                    .map(hit -> ConvertUtils.convert(hit.source(),BookShowDTO.class))
                    .collect(Collectors.toList());
            log.info(books.toString());
            return new PageResult<>(total,books);

        } catch (IOException e) {
            throw new BaseException(ElasticsearchConstant.ES_BOOK_TAG_SEARCH_ERROR);
        }

    }
}
