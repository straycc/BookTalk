package com.cc.talkadmin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cc.talkcommon.Json.JacksonObjectMapper;
import com.cc.talkpojo.Result.TagUpResult;
import com.cc.talkpojo.Result.UploadResult;
import com.cc.talkpojo.dto.BookDTO;
import com.cc.talkpojo.dto.TagDTO;
import com.cc.talkpojo.entity.Book;
import com.cc.talkpojo.entity.BookTagRelation;
import com.cc.talkpojo.entity.Tag;
import com.cc.talkpojo.test.BookTagDTO;
import com.cc.talkserver.admin.mapper.BookAdminMapper;
import com.cc.talkserver.admin.mapper.BookTagAdminMapper;
import com.cc.talkserver.admin.mapper.TagAdminMapper;
import com.cc.talkserver.admin.service.BookAdminService;
import com.cc.talkserver.admin.service.impl.TagAdminServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@SpringBootTest
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
     * 写入book-tag关系表
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


}
