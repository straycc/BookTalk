package com.cc.talkserver.admin.service.impl;
import cn.hutool.json.JSONUtil;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Result;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.cc.talkcommon.constant.BusinessConstant;
import com.cc.talkcommon.constant.ElasticsearchConstant;
import com.cc.talkcommon.constant.RedisCacheConstant;
import com.cc.talkcommon.exception.BaseException;
import com.cc.talkcommon.utils.BuildQueryWrapper;
import com.cc.talkcommon.utils.ConvertUtils;
import com.cc.talkpojo.Result.PageResult;
import com.cc.talkpojo.Result.UploadResult;
import com.cc.talkpojo.dto.BookDTO;
import com.cc.talkpojo.dto.PageBookDTO;
import com.cc.talkpojo.entity.Book;
import com.cc.talkpojo.entity.BookES;
import com.cc.talkpojo.entity.Tag;
import com.cc.talkpojo.vo.BookVO;
import com.cc.talkserver.admin.mapper.BookAdminMapper;
import com.cc.talkserver.admin.mapper.BookTagAdminMapper;
import com.cc.talkserver.admin.mapper.TagAdminMapper;
import com.cc.talkserver.admin.service.BookAdminService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * <p>
 * 图书主表 服务实现类
 * </p>
 *
 * @author cc
 * @since 2025-07-14
 */
@Service
@Slf4j
public class BookAdminServiceImpl extends ServiceImpl<BookAdminMapper, Book> implements BookAdminService {


    @Resource
    private BookAdminMapper bookAdminMapper;

    @Resource
    private ElasticsearchClient elasticsearchClient;

    @Resource
    private TagAdminMapper tagAdminMapper;


    @Resource
    @Qualifier("customStringRedisTemplate")
    private RedisTemplate<String, String> stringRedisTemplate;

    @Resource
    @Qualifier("customObjectRedisTemplate")
    private RedisTemplate<String, Object> objectRedisTemplate;

    /**
     * 单本图书上传
     * @param bookDTO
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bookUpload(BookDTO bookDTO) {

        //1. 检查必要参数是否为空
        if(bookDTO==null || bookDTO.getTitle() == null || bookDTO.getIsbn() == null){
            throw new BaseException("参数有误!");
        }
        //2.检查图书是否已存在
        Book bookIsExist = bookAdminMapper.selectOne(new LambdaQueryWrapper<Book>()
                .eq(Book::getIsbn, bookDTO.getIsbn()));
        if(bookIsExist != null){
            throw new BaseException("图书已存在,请勿重复上传!");
        }

        //3.写入mysql
        Book book = new Book();
        BeanUtils.copyProperties(bookDTO,book);
        bookAdminMapper.insert(book);


        //4. 准备写入 Elasticsearch 的对象
        BookES bookES = ConvertUtils.convert(book, BookES.class);

        try {
            //5. 执行写入
            IndexRequest<BookES> request = IndexRequest.of(i -> i
                    .index(ElasticsearchConstant.ES_BOOK_INDEX)
                    .id(bookES.getId().toString())
                    .document(bookES)
            );
            IndexResponse response = elasticsearchClient.index(request);

            log.info("ES写入成功，索引={}，ID={}", response.index(), response.id());

        } catch (IOException e) {
            log.error("ES写入失败，ID={}，错误信息={}", bookES.getId(), e.getMessage(), e);
            throw new BaseException("写入搜索引擎失败");
        }

    }


    /**
     * 图书批量上传
     * @param bookList
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public UploadResult booksBatchUpload(List<BookDTO> bookList) {

        //1. 基础参数检查
        if(bookList == null || bookList.isEmpty()){
            throw new BaseException("上传图书列表不能为空!");

        }

        //2. 待保存图书筛选
        List<Book> toSaveList = new ArrayList<>();//待存入图书
        List<String> skippedIsbnList = new ArrayList<>();//重复isbn的图书
        List<String> invalidTitleList = new ArrayList<>();//isbn缺失图书


        Set<String> batchIsbnSet = new HashSet<>();
        for (BookDTO bookDTO : bookList) {

            //isbn为空处理
            if (bookDTO.getIsbn() == null || StringUtils.isBlank(bookDTO.getIsbn())) {
                invalidTitleList.add(bookDTO.getTitle() == null ? "(未知图书)" : bookDTO.getTitle());
                continue;
            }

            // 批次内重复
            if (!batchIsbnSet.add(bookDTO.getIsbn())) {
                skippedIsbnList.add(bookDTO.getIsbn());
                continue;
            }

            //isbn重复处理
            Book bookIsExist = bookAdminMapper.selectOne(new LambdaQueryWrapper<Book>()
                    .eq(Book::getIsbn, bookDTO.getIsbn()));

            if(bookIsExist != null){
                skippedIsbnList.add(bookDTO.getIsbn());
                continue;
            }

            //待保存图书
            Book book = new Book();
            BeanUtils.copyProperties(bookDTO,book);
            book.setCreateTime(LocalDateTime.now());
            toSaveList.add(book);
        }

        //3. 批量插入
        if (!toSaveList.isEmpty()) {

            // 3.1 存入mysql
            saveBatch(toSaveList);

            // 3.2 存入ES
            try {
                BulkRequest.Builder br = new BulkRequest.Builder();

                for (Book book : toSaveList) {
                    BookES bookES = ConvertUtils.convert(book, BookES.class);

                    br.operations(op -> op
                            .index(idx -> idx
                                    .index(ElasticsearchConstant.ES_BOOK_INDEX)   // ES 索引名
                                    .id(bookES.getId().toString())
                                    .document(bookES)
                            )
                    );
                }

                BulkResponse result = elasticsearchClient.bulk(br.build());

                if (result.errors()) {
                    log.warn("部分图书同步到 ES 失败:");
                    result.items().stream()
                            .filter(item -> item.error() != null)
                            .forEach(item -> log.warn("写入失败 -> ID: {}, 原因: {}", item.id(), item.error().reason()));
                } else {
                    log.info("成功同步 {} 本图书到 Elasticsearch", toSaveList.size());
                }

            } catch (IOException e) {
                log.error("批量写入 Elasticsearch 异常", e);
                throw new BaseException(ElasticsearchConstant.ES_WRITE_ERROR);
            }
        }
        //4. 返回数据构造
        return new UploadResult(
                toSaveList.size(),
                skippedIsbnList,
                invalidTitleList
        );
    }

    /**
     * 图书信息分页查询
     * @param pageBookDTO
     * @return
     */
    @Override
    public PageResult<BookVO> getBookPage(PageBookDTO pageBookDTO) {
        if(pageBookDTO == null || pageBookDTO.getPageNum() == null || pageBookDTO.getPageSize() == null){
            throw new BaseException(BusinessConstant.PARAM_ERROR);
        }
        PageHelper.startPage(pageBookDTO.getPageNum(), pageBookDTO.getPageSize());


        // 2. 构建查询条件(工具类)
        LambdaQueryWrapper<Book> wrapper = BuildQueryWrapper.buildBookQueryWrapper(pageBookDTO);

        // 3. 执行查询（必须是返回 List<Book>）
        List<Book> booksList = bookAdminMapper.selectList(wrapper);

        // PageHelper 会自动将结果封装成 Page 对象
        Page<Book> pageInfo = (Page<Book>) booksList;

        // 4. 转换为 VO 列表
        List<BookVO> voList = booksList.stream()
                .map(book -> ConvertUtils.convert(book, BookVO.class))
                .collect(Collectors.toList());

        // 5. 封装 PageResult
        return new PageResult<>(pageInfo.getTotal(), voList);

    }

    /**
     * 查询书记详细信息
     * @param id
     * @return
     */
    @Override
    public BookVO getBookDetail(Long id) {
        if(id == null || id <= 0){
            throw  new BaseException(BusinessConstant.PARAM_ERROR);
        }
        Book book = bookAdminMapper.selectById(id);
        if (book == null) {
            throw new BaseException(BusinessConstant.BOOK_NOTEXIST);
        }
        return ConvertUtils.convert(book, BookVO.class);
    }


    /**
     * 图书信息编辑
     * @param id
     * @param bookDTO
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateBookDetail(Long id, BookDTO bookDTO) {

        if(id == null || id <= 0 || bookDTO == null ){
            throw  new BaseException(BusinessConstant.PARAM_ERROR);
        }
        // 检查该图书是否存在
        Book bookInDb = bookAdminMapper.selectById(id);
        if (bookInDb == null) {
            throw new BaseException(BusinessConstant.BOOK_NOTEXIST);
        }

        Book book = new Book();
        BeanUtils.copyProperties(bookDTO,book);
        book.setId(id);
        //  mysql更新
        int rows = bookAdminMapper.updateById(book);
        if (rows == 0) {
            throw new BaseException(BusinessConstant.BOOK_UPDATE_MYSQL_ERROR);
        }

        // Elasticsearch更新
        BookES bookES = ConvertUtils.convert(book, BookES.class);
        try {
            IndexRequest<BookES> indexRequest = IndexRequest.of(
                    idx -> idx
                            .index(ElasticsearchConstant.ES_BOOK_INDEX)   // ES 索引名
                            .id(bookES.getId().toString())
                            .document(bookES)
            );
            IndexResponse response = elasticsearchClient.index(indexRequest);
            log.info("ES写入成功，索引={}，ID={}", response.index(), response.id());

        } catch (IOException e) {
            log.error("ES写入失败", e);
            throw new BaseException(ElasticsearchConstant.ES_WRITE_ERROR);
        }

    }


    /**
     * 删除单本图书
     * @param id
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteById(Long id) {
        if(id == null || id < 0){
            throw new BaseException(BusinessConstant.PARAM_ERROR);
        }
        //1. mysql 删除
        int num = bookAdminMapper.deleteById(id);
        if (num == 0) {
            throw new BaseException(BusinessConstant.BOOK_DELETE_MYSQL_ERROR);
        }

        //2. Elasticsearch 删除
        try {
            DeleteRequest deleteRequest = DeleteRequest.of(d -> d
                    .index(ElasticsearchConstant.ES_BOOK_INDEX)
                    .id(id.toString())
            );

            DeleteResponse response = elasticsearchClient.delete(deleteRequest);

            if (response.result() == Result.NotFound) {
                log.warn("ES中未找到要删除的文档，id: {}", id);
            } else {
                log.info("ES删除成功，id: {}", id);
            }

        } catch (Exception e) {
            log.error("ES删除失败", e);
            throw new BaseException(BusinessConstant.BOOK_DELETE_ES_ERROR);
        }

    }

    /**
     * 批量删除图书
     * @param ids
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deleteByIdS(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BaseException(BusinessConstant.PARAM_ERROR);
        }

        // 1. MySQL 删除
        int num = bookAdminMapper.deleteByIds(ids);
        if (num != ids.size()) {
            throw new BaseException(BusinessConstant.BOOK_DELETE_MYSQL_ERROR);
        }

        // 2. Elasticsearch 批量删除
        try {
            BulkRequest.Builder bulkRequestBuilder = new BulkRequest.Builder();

            for (Long id : ids) {
                bulkRequestBuilder.operations(op -> op
                        .delete(d -> d
                                .index(ElasticsearchConstant.ES_BOOK_INDEX)
                                .id(id.toString())
                        )
                );
            }

            BulkResponse response = elasticsearchClient.bulk(bulkRequestBuilder.build());

            if (response.errors()) {
                log.error("部分 ES 删除失败：");
                response.items().stream()
                        .filter(item -> item.error() != null)
                        .forEach(item -> log.error("失败ID: {}, 原因: {}", item.id(), item.error().reason()));

                throw new BaseException(BusinessConstant.BOOK_DELETE_ES_ERROR);
            } else {
                log.info("成功删除 {} 条数据（ES）", ids.size());
            }

        } catch (Exception e) {
            log.error("ES 批量删除失败", e);
            throw new BaseException(BusinessConstant.BOOK_DELETE_ES_ERROR);
        }

    }


    /**
     * 热门图书缓存
     */
    @Override
    //TODO 缓存更新逻辑，目前采整表重建的方式，待优化
    public void refreshHotBooksCache() {

        //1. 获取本月
        String month = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));

        //2. 热门标签 ID 查询
        List<Tag> hotTags = tagAdminMapper.selectHotTagsByUsageCount(RedisCacheConstant.HOT_TAGS_COUNT);
        List<Long> hotTagIds = hotTags.stream().map(Tag::getId).collect(Collectors.toList());

        for (Long hotTagId : hotTagIds) {
            //3. 获取热门标签图书列表
            List<Book> hotBooks = bookAdminMapper.findHotBooksByMonthAndTag(month, hotTagId, RedisCacheConstant.HOT_BOOKS_COUNT);


            //4. 缓存热门图书id列表
            String hotBooksKey = RedisCacheConstant.HOT_BOOKS_KEY_PREFIX + month + ":" + hotTagId;

            if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(hotBooksKey))) {
                stringRedisTemplate.delete(hotBooksKey);
            }
            if (!hotBooks.isEmpty()) {
                List<String> bookIds = hotBooks.stream()
                        .map(book -> String.valueOf(book.getId()))
                        .collect(Collectors.toList());
                stringRedisTemplate.opsForList().rightPushAll(hotBooksKey, bookIds);
                stringRedisTemplate.expire(hotBooksKey, RedisCacheConstant.CACHE_EXPIRE_SECONDS, TimeUnit.SECONDS);
            }

            //5. 热门图书详细信息
            String hashKey = RedisCacheConstant.BOOK_DETAIL_KEY_PREFIX + hotTagId;
            objectRedisTemplate.delete(hashKey); // 删除旧hash缓存

            if (!hotBooks.isEmpty()) {
                for (Book book : hotBooks) {
                    objectRedisTemplate.opsForHash().put(hashKey, String.valueOf(book.getId()), book);
                }
                objectRedisTemplate.expire(hashKey, RedisCacheConstant.CACHE_EXPIRE_SECONDS, TimeUnit.SECONDS);
            }
        }

    }
}
