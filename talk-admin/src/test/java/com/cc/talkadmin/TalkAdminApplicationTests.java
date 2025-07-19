package com.cc.talkadmin;

import com.cc.talkcommon.Json.JacksonObjectMapper;
import com.cc.talkpojo.Result.UploadResult;
import com.cc.talkpojo.dto.BookDTO;
import com.cc.talkserver.admin.service.BookAdminService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;

@SpringBootTest
class TalkAdminApplicationTests {

    @Autowired
    private BookAdminService bookAdminService;

    @Test
    public void testBatchUploadFromJson() throws Exception {
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

}
