package com.cc.talkadmin;

import cn.hutool.crypto.SecureUtil;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.cc.talkcommon.Json.JacksonObjectMapper;
import com.cc.talkcommon.constant.BusinessConstant;
import com.cc.talkcommon.constant.ElasticsearchConstant;
import com.cc.talkcommon.exception.BaseException;
import com.cc.talkpojo.Result.TagUpResult;
import com.cc.talkpojo.Result.UploadResult;
import com.cc.talkpojo.dto.BookDTO;
import com.cc.talkpojo.dto.TagDTO;
import com.cc.talkpojo.dto.admin.AdminBookReviewDTO;
import com.cc.talkpojo.entity.*;
import com.cc.talkpojo.enums.ReviewType;
import com.cc.talkpojo.test.BookTagDTO;
import com.cc.talkpojo.test.BookTagRelationDTO;
import com.cc.talkpojo.test.Book_Reviews_Test;
import com.cc.talkserver.admin.mapper.BookAdminMapper;
import com.cc.talkserver.admin.mapper.BookTagAdminMapper;
import com.cc.talkserver.admin.mapper.TagAdminMapper;
import com.cc.talkserver.admin.mapper.UserAdminMapper;
import com.cc.talkserver.admin.service.BookAdminService;
import com.cc.talkserver.admin.service.ReviewAdminService;
import com.cc.talkserver.admin.service.impl.TagAdminServiceImpl;
import com.cc.talkserver.user.mapper.UserMapper;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@SpringBootTest
@Slf4j
class TalkAdminApplicationTests {

    @Resource
    private BookAdminService bookAdminService;
    @Resource
    private TagAdminServiceImpl tagAdminServiceImpl;

    @Resource
    private BookAdminMapper bookAdminMapper;

    @Resource
    private TagAdminMapper tagAdminMapper;

    @Resource
    private BookTagAdminMapper bookTagAdminMapper;
    @Autowired
    private ElasticsearchClient elasticsearchClient;

    /**
     * 书籍数据批量上传
     * @throws Exception
     */
    @Test
    public void testBatchUploadBooks() throws Exception {
        // 1. 获取资源文件流（classpath 下的 json/books.json）
        InputStream is = this.getClass().getClassLoader().getResourceAsStream("json/books.json");

        if (is == null) {
            throw new FileNotFoundException("未找到资源文件：json/books.json");
        }

        // 2. 转为 List<BookDTO>
        ObjectMapper mapper = new JacksonObjectMapper();
        List<BookDTO> bookList = mapper.readValue(is, new TypeReference<>() {});

        // 3. 批量上传
        UploadResult result = bookAdminService.booksBatchUpload(bookList);

        // 4. 打印结果
        System.out.println("导入完成，总数：" + bookList.size());
        System.out.println("成功：" + result.getSuccessCount());
    }


    /**
     * 标签数据批量上传
     */
    @Test
    public void testBatchUploadTags() throws Exception {
        // 1. 获取资源文件流（classpath 下的 json/tags.json）
        InputStream is = this.getClass().getClassLoader().getResourceAsStream("json/tags.json");

        if (is == null) {
            throw new FileNotFoundException("未找到资源文件：json/tags.json");
        }

        // 2. 转为 List<TagDTO>
        ObjectMapper mapper = new JacksonObjectMapper();
        List<TagDTO> tagList = mapper.readValue(is, new TypeReference<>() {});

        // 3. 批量上传
        TagUpResult result = tagAdminServiceImpl.tagsBatchAdd(tagList);

        // 4. 打印结果
        System.out.println("导入完成，总数：" + tagList.size());
        System.out.println("成功：" + result.getSuccessCount());

    }


    /**
     * MYSQL写入book-tag关系表
     * @throws Exception
     */
    @Test
    public void testBatchUploadBookTags() throws Exception {
        // 1. 读取资源文件
        InputStream is = this.getClass().getClassLoader().getResourceAsStream("json/book_tag.json");
        if (is == null) {
            throw new FileNotFoundException("未找到资源文件：json/book_tag.json");
        }

        // 2. 反序列化为 List<BookTagDTO>
        ObjectMapper mapper = new ObjectMapper();
        List<BookTagDTO> bookTagDTOList = mapper.readValue(is, new TypeReference<List<BookTagDTO>>() {});

        // 3. 遍历处理
        for (BookTagDTO bookTagDTO : bookTagDTOList) {
            String isbn = bookTagDTO.getIsbn();
            List<String> tagNames = bookTagDTO.getTag();

            // 查找书籍
            Book book = bookAdminMapper.selectOne(new QueryWrapper<Book>().eq("isbn", isbn));
            if (book == null) {
                System.out.println("书籍不存在，ISBN=" + isbn);
                continue;
            }
            Long bookId = book.getId();

            // 查找已有标签
            List<Tag> tags = tagAdminMapper.selectList(new QueryWrapper<Tag>().in("name", tagNames));
            Set<String> existTagNames = tags.stream().map(Tag::getName).collect(Collectors.toSet());

            // 新增不存在的标签
            for (String tagName : tagNames) {
                if (!existTagNames.contains(tagName)) {
                    Tag newTag = new Tag();
                    newTag.setName(tagName);
                    newTag.setCreatorId(0L); // 管理员ID示例
                    newTag.setCreateTime(LocalDateTime.now());
                    newTag.setUpdateTime(LocalDateTime.now());
                    tagAdminMapper.insert(newTag);
                    tags.add(newTag);
                }
            }

            // 写入关联表，避免重复
            for (Tag tag : tags) {
                Long count = bookTagAdminMapper.selectCount(new QueryWrapper<BookTagRelation>()
                        .eq("book_id", bookId)
                        .eq("tag_id", tag.getId()));
                if (count != null && count > 0) continue;

                BookTagRelation bookTag = new BookTagRelation();
                bookTag.setBookId(bookId);
                bookTag.setTagId(tag.getId());
                bookTag.setCreateTime(LocalDateTime.now());
                bookTag.setUpdateTime(LocalDateTime.now());
                bookTagAdminMapper.insert(bookTag);
            }
        }

        System.out.println("书籍标签批量上传完成");
    }



    /**
     * 将book-tag关系表写入ES
     */
    @Test
    public void testBatchUploadBookTagES() throws Exception {
        // 1. 查出关系表的所有数据
        List<BookTagRelation> relations = bookTagAdminMapper.selectList(null);
        if (relations.isEmpty()) {
            System.out.println("没有找到任何 book-tag 关系数据");
            return;
        }

        // 2. 批量构建 ES 文档
        List<BookTagRelationDTO> docs = new ArrayList<>();
        for (BookTagRelation relation : relations) {
            Tag tag = tagAdminMapper.selectById(relation.getTagId());
            if (tag == null) continue;

            BookTagRelationDTO doc = new BookTagRelationDTO();
            doc.setBookId(relation.getBookId());
            doc.setTagId(tag.getId());
            doc.setTagName(tag.getName());
            docs.add(doc);
        }

        // 3. 批量写入 ES
        BulkRequest.Builder br = new BulkRequest.Builder();
        for (BookTagRelationDTO doc : docs) {
            String id = doc.getBookId() + "_" + doc.getTagId(); // 组合主键
            br.operations(op -> op
                    .index(idx -> idx
                            .index(ElasticsearchConstant.ES_BOOK_TAG_INDEX)
                            .id(id)
                            .document(doc)
                    )
            );
        }
        BulkResponse bulkResponse = elasticsearchClient.bulk(br.build());

        // 4. 检查结果
        if (bulkResponse.errors()) {
            System.err.println("部分数据写入 ES 失败");
            bulkResponse.items().forEach(item -> {
                if (item.error() != null) {
                    System.err.println(item.error().reason());
                }
            });
        } else {
            System.out.println("成功写入 ES 文档数：" + docs.size());
        }
    }


    /**
     * 批量插入评论
     */
    @Autowired
    private ReviewAdminService reviewAdminService;

    @Autowired
    private UserAdminMapper userAdminMapper;       // 查询或新增用户ID


    @Test
    @Commit
    public void batchInsertReviews() throws Exception {
        // 1. 读取资源文件
        InputStream is = this.getClass().getClassLoader().getResourceAsStream("json/book_reviews.json");
        if (is == null) {
            throw new FileNotFoundException("未找到资源文件：json/book_reviews.json");
        }

        // 2. 反序列化为 List<Book_Reviews_Test>
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(JsonParser.Feature.ALLOW_NON_NUMERIC_NUMBERS, true);
        // 注册 JavaTimeModule 以支持 Java 8 日期时间类型
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        List<Book_Reviews_Test> bookReviewsTests = null;

        try {
            bookReviewsTests = mapper.readValue(is, new TypeReference<List<Book_Reviews_Test>>() {});
        } catch (Exception e) {
            log.error("反序列化 JSON 数据失败", e);
            throw new RuntimeException("反序列化失败", e);
        }

        // 3. 遍历每条书籍评论
        for (Book_Reviews_Test bookReviews : bookReviewsTests) {

            int type = BusinessConstant.REVIEW_TYPE_SHORT; // 默认短评
            if (StringUtils.isNotBlank(bookReviews.getContent()) && bookReviews.getContent().length() > 50) {
                type = BusinessConstant.REVIEW_TYPE_LONG; // 长评论
            }

            // 4. 根据 bookName 查找 bookId
            LambdaQueryWrapper<Book> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(StringUtils.isNotBlank(bookReviews.getBookName()), Book::getTitle, bookReviews.getBookName());

            // 使用 selectList() 查询可能有多个结果
            List<Book> books = bookAdminMapper.selectList(queryWrapper);

            // 检查查询结果
            if (books.size() > 1) {
                // 如果查询结果大于 1，说明存在重复的数据
                log.warn("找到多个书籍：{}", bookReviews.getBookName());
                continue; // 跳过当前评论，继续下一个
            } else if (books.isEmpty()) {
                // 如果没有找到书籍
                log.warn("书籍不存在：{}", bookReviews.getBookName());
                continue; // 跳过当前评论，继续下一个
            }

            // 如果只有一个结果
            Book book = books.get(0);
            Long bookId = book.getId();


            // 5. 根据 userName 查找或创建用户
            User user = userAdminMapper.selectOne(new QueryWrapper<User>().eq("username", bookReviews.getUserName()));
            Long userId = null;

            if (user == null) {
                // 创建新用户
                User user1 = new User();
                user1.setUsername(bookReviews.getUserName());
                user1.setPassword(SecureUtil.md5(BusinessConstant.USER_DEFAULT_PASSWORD)); // 默认密码加密
                user1.setCreateTime(LocalDateTime.now());
                user1.setUpdateTime(LocalDateTime.now());
                userAdminMapper.insert(user1);
                userId = user1.getId();
            } else {
                userId = user.getId();
            }

            // 6. 创建 BookReview 并存入数据库
            AdminBookReviewDTO bookReviewDTO = AdminBookReviewDTO.builder()
                    .bookId(bookId)
                    .userId(userId)
                    .content(bookReviews.getContent())
                    .title(bookReviews.getTitle())
                    .score(bookReviews.getScore().intValue())
                    .likeCount(bookReviews.getLikeCount().intValue())
                    .status(2) // 默认状态
                    .createTime(bookReviews.getDate() != null ? bookReviews.getDate().atStartOfDay() : LocalDateTime.now()) // 设置时间
                    .type(type)
                    .build();

            try {
                reviewAdminService.reviewAdd(bookId, bookReviewDTO);
            } catch (Exception e) {
                log.error("插入评论失败，书籍ID：{}, 用户ID：{}", bookId, userId, e);
            }
        }
    }



}
