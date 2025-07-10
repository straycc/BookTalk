package com.cc.talkadmin.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cc.talkadmin.service.IBookService;
import com.cc.talkcommon.constant.ElasticsearchConstant;
import com.cc.talkcommon.exception.BaseException;
import com.cc.talkcommon.utils.ConvertUtils;
import com.cc.talkpojo.Result.PageResult;
import com.cc.talkpojo.Result.UploadResult;
import com.cc.talkpojo.dto.BookDTO;
import com.cc.talkpojo.dto.BookEsDTO;
import com.cc.talkpojo.dto.PageBookDTO;
import com.cc.talkpojo.entity.Book;
import com.cc.talkadmin.mapper.BookMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cc.talkpojo.entity.BookCategory;
import com.cc.talkpojo.vo.BookVO;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 图书主表 服务实现类
 * </p>
 *
 * @author cc
 * @since 2025-07-05
 */
@Service
@Slf4j
public class BookServiceImpl extends ServiceImpl<BookMapper, Book> implements IBookService {


    @Resource
    private BookMapper bookMapper;


    @Resource
    private RestHighLevelClient restHighLevelClient;

    /**
     * 单本图书上传
     * @param bookDTO
     */
    @Override
    public void bookUpload(BookDTO bookDTO) {

        //1. 检查必要参数是否为空
        if(bookDTO==null || bookDTO.getTitle() == null || bookDTO.getIsbn() == null){
            throw new BaseException("参数有误!");
        }
        //2.检查图书是否已存在
        Book bookIsExist = bookMapper.selectOne(new LambdaQueryWrapper<Book>()
                .eq(Book::getIsbn, bookDTO.getIsbn()));
        if(bookIsExist != null){
            throw new BaseException("图书已存在,请勿重复上传!");
        }

        //3.写入mysql
        Book book = new Book();
        BeanUtils.copyProperties(bookDTO,book);
        bookMapper.insert(book);


        //4. 准备写入 Elasticsearch 的对象
        BookEsDTO bookEs = BookEsDTO.builder()
                .id(book.getId())
                .isbn(book.getIsbn())
                .originalTitle(book.getOriginalTitle())
                .title(book.getTitle())
                .description(book.getDescription())
                .author(book.getAuthor())
                .authorCountry(book.getAuthorCountry())
                .publisher(book.getPublisher())
                .producer(book.getProducer())
                .translator(book.getAuthor())
                .publishDate(book.getPublishDate())
                .price(book.getPrice())
                .coverUrl(book.getCoverUrl())
                .categoryId(book.getCategoryId())
                .coverUrl(book.getCoverUrl())
                .build();


        //5. 写入Elasticsearch
        try {
            IndexRequest request = new IndexRequest(ElasticsearchConstant.ES_BOOK_INDEX)
                    .id(book.getId().toString())
                    .source(JSONUtil.toJsonStr(bookEs), XContentType.JSON);

            restHighLevelClient.index(request, RequestOptions.DEFAULT);
        } catch (IOException e) {
            log.error("ES写入失败", e);
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

        for (BookDTO bookDTO : bookList) {

            //isbn为空处理
            if (bookDTO.getIsbn() == null || StringUtils.isBlank(bookDTO.getIsbn())) {
                invalidTitleList.add(bookDTO.getTitle() == null ? "(未知图书)" : bookDTO.getTitle());
                continue;
            }

            //isbn重复处理
            Book bookIsExist = bookMapper.selectOne(new LambdaQueryWrapper<Book>()
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

            // 3.2 构建 BulkRequest 写入 ES
            BulkRequest bulkRequest = new BulkRequest();

            for (Book book : toSaveList) {
                // 将 Book 转为 BookEsDTO
                BookEsDTO bookEs = ConvertUtils.convert(book, BookEsDTO.class);

                IndexRequest indexRequest = new IndexRequest("book_index")
                        .id(bookEs.getId().toString())
                        .source(JSONUtil.toJsonStr(bookEs), XContentType.JSON);

                bulkRequest.add(indexRequest);
            }

            try {
                BulkResponse bulkResponse = restHighLevelClient.bulk(bulkRequest, RequestOptions.DEFAULT);

                if (bulkResponse.hasFailures()) {
                    log.warn("部分图书同步到 ES 失败: {}",bulkResponse.buildFailureMessage());
                }
            } catch (IOException e) {
                log.error("批量写入 ES 异常", e);
                throw new BaseException("部分图书写入 ES 失败");
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
            throw new BaseException("参数有误!");
        }
        PageHelper.startPage(pageBookDTO.getPageNum(), pageBookDTO.getPageSize());


        // 2. 构建查询条件
         LambdaQueryWrapper<Book> wrapper = buildQueryWrapper(pageBookDTO);

        // 3. 执行查询（必须是返回 List<Book>）
        List<Book> booksList = bookMapper.selectList(wrapper);

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
            throw  new BaseException("参数id有误!");
        }
        Book book = bookMapper.selectById(id);
        if (book == null) {
            throw new BaseException("图书不存在!");
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
            throw  new BaseException("参数有误!");
        }
        // 检查该图书是否存在
        Book bookInDb = bookMapper.selectById(id);
        if (bookInDb == null) {
            throw new BaseException("图书不存在，无法更新!");
        }

        Book book = new Book();
        BeanUtils.copyProperties(bookDTO,book);
        book.setId(id);
        //  mysql更新
        int rows = bookMapper.updateById(book);
        if (rows == 0) {
            throw new BaseException("mysql更新失败!");
        }

        // Elasticsearch更新
        BookEsDTO bookEs = ConvertUtils.convert(book, BookEsDTO.class);
        try {
            IndexRequest request = new IndexRequest(ElasticsearchConstant.ES_BOOK_INDEX)
                    .id(book.getId().toString())
                    .source(JSONUtil.toJsonStr(bookEs), XContentType.JSON);

            restHighLevelClient.index(request, RequestOptions.DEFAULT);
        } catch (IOException e) {
            log.error("ES写入失败", e);
            throw new BaseException("写入搜索引擎失败");
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
            throw new BaseException("参数有误!");
        }
        //1. mysql 删除
        int num = bookMapper.deleteById(id);
        if (num == 0) {
            throw new BaseException("数据库中不存在该图书，删除失败");
        }

        //2. Elasticsearch 删除
        try{
            DeleteRequest deleteRequest = new DeleteRequest(ElasticsearchConstant.ES_BOOK_INDEX, id.toString());
            restHighLevelClient.delete(deleteRequest, RequestOptions.DEFAULT);
        }catch (Exception e){
            log.error("Es删除失败",e);
            throw new BaseException("删除搜索引擎数据失败");
        }
    }

    /**
     * 批量删除图书
     * @param ids
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deleteByIdS(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BaseException("列表为空!");
        }

        // 1. MySQL 删除
        int num = bookMapper.deleteByIds(ids);
        if (num != ids.size()) {
            throw new BaseException("批量删除失败!");
        }

        // 2. Elasticsearch 批量删除
        try {
            BulkRequest bulkRequest = new BulkRequest();
            for (Integer id : ids) {
                DeleteRequest deleteRequest = new DeleteRequest(ElasticsearchConstant.ES_BOOK_INDEX, id.toString());
                bulkRequest.add(deleteRequest);
            }

            BulkResponse bulkResponse = restHighLevelClient.bulk(bulkRequest, RequestOptions.DEFAULT);

            if (bulkResponse.hasFailures()) {
                log.error("部分 ES 删除失败：{}", bulkResponse.buildFailureMessage());
                throw new BaseException("部分搜索引擎数据删除失败");
            }

        } catch (Exception e) {
            log.error("ES 批量删除失败", e);
            throw new BaseException("搜索引擎批量删除失败");
        }
    }


    /**
     * 构建查询条件包装器
     */
    private LambdaQueryWrapper<Book> buildQueryWrapper(PageBookDTO pageBookDTO) {
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
    private SFunction<Book, ?> resolveSortField(String fieldName) {
        switch (fieldName) {
            case "title": return Book::getTitle;
            case "publishDate": return Book::getPublishDate;
            case "price": return Book::getPrice;
            default: return Book::getId; // 默认排序字段
        }
    }

}
